import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/** Lo que se hace con los enlaces que llegan por correo, sin necesidad de sesión. */
@Injectable({ providedIn: 'root' })
export class AccesoApi {
  private readonly http = inject(HttpClient);

  verificarEmail(token: string): Observable<void> {
    return this.http.post<void>('/api/auth/verificacion', { token });
  }

  /** Responde igual exista o no una cuenta con ese email. */
  pedirCambioDePassword(email: string): Observable<void> {
    return this.http.post<void>('/api/auth/recuperacion', { email });
  }

  restablecerPassword(token: string, password: string): Observable<void> {
    return this.http.post<void>('/api/auth/restablecimiento', { token, password });
  }
}
