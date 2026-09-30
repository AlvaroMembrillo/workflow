import { HttpClient, HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, map, Observable, of, share, tap } from 'rxjs';

import { leerToken } from './jwt';
import { LoginRequest, RegistroRequest, TokenResponse, UsuarioSesion } from './modelos';

/**
 * Marca en localStorage de que este navegador tiene una sesión abierta. No es un secreto: solo sirve para
 * saber si merece la pena pedir un token al abrir la web y para avisar a las demás pestañas.
 */
const CLAVE_PISTA = 'workflow.sesion';
/** El token de acceso se renueva un poco antes de caducar, para que ninguna petición salga con él caducado. */
const ANTELACION_MAXIMA = 30_000;
/** setTimeout desborda con esperas mayores (unos 24,8 días) y se dispararía al instante. */
const MAXIMA_ESPERA_TEMPORIZADOR = 2 ** 31 - 1;

/**
 * Sesión del usuario. El token de acceso dura minutos y solo vive en memoria; lo que mantiene la sesión
 * al recargar la página es el token de refresco, que va en una cookie HttpOnly que este código no puede
 * leer: se usa pidiendo a la API un token de acceso nuevo.
 */
@Injectable({ providedIn: 'root' })
export class Sesion {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly tokenActual = signal<string | null>(null);
  private readonly cerradaPorCaducidad = signal(false);
  private temporizador?: ReturnType<typeof setTimeout>;
  private renovacionEnCurso?: Observable<string>;

  readonly token = this.tokenActual.asReadonly();
  readonly usuario = computed<UsuarioSesion | null>(() => {
    const token = this.tokenActual();
    return token ? leerToken(token) : null;
  });
  readonly iniciada = computed(() => this.usuario() !== null);
  readonly rol = computed(() => this.usuario()?.rol ?? null);
  /** true si la última sesión terminó porque caducó, para explicarlo en la pantalla de acceso. */
  readonly caducada = this.cerradaPorCaducidad.asReadonly();

  constructor() {
    // Si se inicia o se cierra sesión en otra pestaña, esta se pone al día
    const alCambiarAlmacen = (evento: StorageEvent) => {
      if (evento.key !== CLAVE_PISTA) {
        return;
      }
      if (evento.newValue === null) {
        this.cerradaPorCaducidad.set(false);
        this.establecer(null);
      } else if (!this.iniciada()) {
        this.restaurar().subscribe();
      }
    };
    window.addEventListener('storage', alCambiarAlmacen);
    inject(DestroyRef).onDestroy(() => {
      window.removeEventListener('storage', alCambiarAlmacen);
      clearTimeout(this.temporizador);
    });
  }

  entrar(datos: LoginRequest): Observable<UsuarioSesion> {
    return this.http
      .post<TokenResponse>('/api/auth/login', datos)
      .pipe(map((respuesta) => this.iniciar(respuesta.accessToken)));
  }

  registrar(datos: RegistroRequest): Observable<UsuarioSesion> {
    return this.http
      .post<TokenResponse>('/api/auth/registro', datos)
      .pipe(map((respuesta) => this.iniciar(respuesta.accessToken)));
  }

  /**
   * Al abrir la web: si este navegador tenía una sesión, la recupera con la cookie de refresco.
   * Emite si hay sesión. No falla nunca, para no impedir que la aplicación arranque.
   */
  restaurar(): Observable<boolean> {
    if (!hayPista()) {
      return of(false);
    }
    return this.renovar().pipe(
      map(() => true),
      catchError((error: unknown) => {
        if (esNoAutorizado(error)) {
          // La sesión caducó mientras la web estaba cerrada
          this.cerradaPorCaducidad.set(true);
          this.establecer(null);
        }
        return of(false);
      }),
    );
  }

  /**
   * Pide otro token de acceso con la cookie de refresco. Si varias peticiones lo necesitan a la vez,
   * comparten la misma llamada: el token de refresco solo se puede canjear una vez.
   */
  renovar(): Observable<string> {
    this.renovacionEnCurso ??= this.http.post<TokenResponse>('/api/auth/refresco', null).pipe(
      map((respuesta) => respuesta.accessToken),
      tap((token) => this.establecer(token)),
      finalize(() => (this.renovacionEnCurso = undefined)),
      share(),
    );
    return this.renovacionEnCurso;
  }

  /** Cierre de sesión pedido por el usuario. */
  cerrar(): void {
    const habiaSesion = this.iniciada();
    this.cerradaPorCaducidad.set(false);
    this.establecer(null);
    if (habiaSesion) {
      // Invalida el token de refresco en el servidor y borra la cookie. Si falla, la sesión de esta
      // pestaña ya está cerrada igualmente
      this.http.post('/api/auth/salida', null).subscribe({ error: () => undefined });
    }
  }

  /**
   * La sesión ya no vale: ha caducado o el backend la ha rechazado. La cierra y vuelve a evaluar la ruta
   * actual: si necesitaba sesión, su guard lleva a la pantalla de acceso y después se vuelve a esta página.
   */
  caducar(): void {
    this.cerradaPorCaducidad.set(true);
    this.establecer(null);
    void this.router.navigateByUrl(this.router.url, { onSameUrlNavigation: 'reload' });
  }

  private iniciar(token: string): UsuarioSesion {
    this.cerradaPorCaducidad.set(false);
    this.establecer(token);
    const usuario = this.usuario();
    if (!usuario) {
      throw new Error('La API ha devuelto un token que no se puede leer');
    }
    return usuario;
  }

  private establecer(token: string | null): void {
    const usuario = token ? leerToken(token) : null;
    const vida = usuario ? usuario.expira - Date.now() : 0;
    const vigente = vida > 0 ? token : null;

    clearTimeout(this.temporizador);
    this.tokenActual.set(vigente);
    guardarPista(vigente !== null);
    if (vigente) {
      const espera = vida - Math.min(ANTELACION_MAXIMA, vida / 2);
      this.temporizador = setTimeout(
        () => this.renovarAntesDeCaducar(),
        Math.min(espera, MAXIMA_ESPERA_TEMPORIZADOR),
      );
    }
  }

  private renovarAntesDeCaducar(): void {
    this.renovar().subscribe({
      error: (error: unknown) => {
        if (esNoAutorizado(error)) {
          this.caducar();
          return;
        }
        // Sin conexión: el token actual sigue valiendo hasta que caduque
        const restante = (this.usuario()?.expira ?? 0) - Date.now();
        this.temporizador = setTimeout(() => this.caducar(), Math.max(restante, 0));
      },
    });
  }
}

function esNoAutorizado(error: unknown): boolean {
  return error instanceof HttpErrorResponse && error.status === HttpStatusCode.Unauthorized;
}

// localStorage puede no estar disponible (modo privado, almacenamiento bloqueado): la sesión funciona igual
// pero no se recupera al recargar.
function hayPista(): boolean {
  try {
    return localStorage.getItem(CLAVE_PISTA) !== null;
  } catch {
    return false;
  }
}

function guardarPista(haySesion: boolean): void {
  try {
    if (haySesion === hayPista()) {
      return;
    }
    if (haySesion) {
      localStorage.setItem(CLAVE_PISTA, '1');
    } else {
      localStorage.removeItem(CLAVE_PISTA);
    }
  } catch {
    // Sin almacenamiento persistente
  }
}
