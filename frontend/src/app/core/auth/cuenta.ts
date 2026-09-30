import { HttpClient, httpResource } from '@angular/common/http';
import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { Usuario } from './modelos';
import { Sesion } from './sesion';

/**
 * La cuenta del usuario con sesión iniciada: sus datos (que no van en el token, como si ha verificado
 * el email) y lo que puede cambiar de ella.
 */
@Injectable({ providedIn: 'root' })
export class Cuenta {
  private readonly http = inject(HttpClient);
  private readonly sesion = inject(Sesion);

  private readonly idUsuario = computed(() => this.sesion.usuario()?.id ?? null);

  private readonly version = signal(0);

  /** Se piden al iniciar sesión y se vuelven a pedir si cambia el usuario. */
  readonly datos = httpResource<Usuario>(() => {
    this.version();
    return this.idUsuario() ? { url: '/api/usuarios/me' } : undefined;
  });

  readonly emailSinVerificar = computed(
    () => this.datos.hasValue() && !this.datos.value().emailVerificado,
  );

  /**
   * Vuelve a pedir los datos. Si ya había una petición en curso la descarta, porque su respuesta
   * puede ser anterior al cambio que motiva la recarga (por ejemplo, verificar el email).
   */
  recargar(): void {
    this.version.update((version) => version + 1);
  }

  constructor() {
    // Lo normal es confirmar el email en otra pestaña (la del correo). Al volver a esta, se comprueba
    // si ya está confirmado para quitar los avisos sin tener que recargar la página
    const alVolver = () => {
      if (document.visibilityState === 'visible' && this.emailSinVerificar()) {
        this.recargar();
      }
    };
    document.addEventListener('visibilitychange', alVolver);
    inject(DestroyRef).onDestroy(() => document.removeEventListener('visibilitychange', alVolver));
  }

  reenviarVerificacion(): Observable<void> {
    return this.http.post<void>('/api/auth/verificacion/reenvio', null);
  }

  cambiarAvisosPorCorreo(avisosPorCorreo: boolean): Observable<Usuario> {
    return this.http
      .patch<Usuario>('/api/usuarios/me', { avisosPorCorreo })
      .pipe(tap((usuario) => this.datos.set(usuario)));
  }

  cambiarNombre(nombre: string): Observable<Usuario> {
    return this.http
      .patch<Usuario>('/api/usuarios/me', { nombre })
      .pipe(tap((usuario) => this.datos.set(usuario)));
  }

  /** Copia de todos los datos personales del usuario, en un fichero JSON. */
  descargarDatos(): Observable<Blob> {
    return this.http.get('/api/usuarios/me/datos', { responseType: 'blob' });
  }

  /** Borra la cuenta y todos sus datos. Hay que confirmar con la contraseña. No se puede deshacer. */
  borrar(password: string): Observable<void> {
    return this.http.post<void>('/api/usuarios/me/baja', { password });
  }

  cambiarPassword(passwordActual: string, passwordNueva: string): Observable<void> {
    return this.http.post<void>('/api/usuarios/me/password', { passwordActual, passwordNueva });
  }
}
