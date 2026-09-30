import { HttpClient, httpResource } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
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

  reenviarVerificacion(): Observable<void> {
    return this.http.post<void>('/api/auth/verificacion/reenvio', null);
  }

  cambiarAvisosPorCorreo(avisosPorCorreo: boolean): Observable<Usuario> {
    return this.http
      .patch<Usuario>('/api/usuarios/me', { avisosPorCorreo })
      .pipe(tap((usuario) => this.datos.set(usuario)));
  }

  cambiarPassword(passwordActual: string, passwordNueva: string): Observable<void> {
    return this.http.post<void>('/api/usuarios/me/password', { passwordActual, passwordNueva });
  }
}
