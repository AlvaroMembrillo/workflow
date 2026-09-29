import { HttpClient } from '@angular/common/http';
import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { map, Observable } from 'rxjs';

import { leerToken } from './jwt';
import { LoginRequest, RegistroRequest, TokenResponse, UsuarioSesion } from './modelos';

const CLAVE_ALMACEN = 'workflow.token';
/** setTimeout desborda con esperas mayores (unos 24,8 días) y se dispararía al instante. */
const MAXIMA_ESPERA_TEMPORIZADOR = 2 ** 31 - 1;

/**
 * Sesión del usuario: guarda el JWT, expone quién ha iniciado sesión y la cierra cuando el token caduca.
 * El token se guarda en localStorage para mantener la sesión al recargar la página.
 */
@Injectable({ providedIn: 'root' })
export class Sesion {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly tokenActual = signal<string | null>(null);
  private readonly cerradaPorCaducidad = signal(false);
  private temporizador?: ReturnType<typeof setTimeout>;

  readonly token = this.tokenActual.asReadonly();
  readonly usuario = computed<UsuarioSesion | null>(() => {
    const token = this.tokenActual();
    return token ? leerToken(token) : null;
  });
  readonly iniciada = computed(() => this.usuario() !== null);
  readonly rol = computed(() => this.usuario()?.rol ?? null);
  /** true si la última sesión terminó porque caducó el token, para explicarlo en la pantalla de acceso. */
  readonly caducada = this.cerradaPorCaducidad.asReadonly();

  constructor() {
    this.establecer(leerDelAlmacen(), false);

    // Si se inicia o se cierra sesión en otra pestaña, esta se pone al día
    const alCambiarAlmacen = (evento: StorageEvent) => {
      if (evento.key === CLAVE_ALMACEN) {
        this.establecer(evento.newValue, false);
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

  /** Cierre de sesión pedido por el usuario. */
  cerrar(): void {
    this.cerradaPorCaducidad.set(false);
    this.establecer(null);
  }

  /**
   * El token ha caducado o el backend lo ha rechazado. Cierra la sesión y vuelve a evaluar la ruta actual:
   * si necesitaba sesión, su guard lleva a la pantalla de acceso y después se vuelve a esta página.
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

  private establecer(token: string | null, guardar = true): void {
    const usuario = token ? leerToken(token) : null;
    const vigente = usuario && usuario.expira > Date.now() ? token : null;

    clearTimeout(this.temporizador);
    this.tokenActual.set(vigente);
    if (guardar || (token && !vigente)) {
      guardarEnAlmacen(vigente);
    }
    if (vigente && usuario) {
      const espera = Math.min(usuario.expira - Date.now(), MAXIMA_ESPERA_TEMPORIZADOR);
      this.temporizador = setTimeout(() => this.caducar(), espera);
    }
  }
}

// localStorage puede no estar disponible (modo privado, almacenamiento bloqueado): la sesión funciona igual
// pero no se conserva al recargar.
function leerDelAlmacen(): string | null {
  try {
    return localStorage.getItem(CLAVE_ALMACEN);
  } catch {
    return null;
  }
}

function guardarEnAlmacen(token: string | null): void {
  try {
    if (token) {
      localStorage.setItem(CLAVE_ALMACEN, token);
    } else {
      localStorage.removeItem(CLAVE_ALMACEN);
    }
  } catch {
    // Sin almacenamiento persistente
  }
}
