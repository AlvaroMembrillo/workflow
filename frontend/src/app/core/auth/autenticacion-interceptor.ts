import {
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
  HttpStatusCode,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';

import { Sesion } from './sesion';

/** Endpoints que abren, renuevan o cierran la sesión: se identifican por sí mismos, sin token de acceso. */
const SIN_TOKEN = [
  '/api/auth/login',
  '/api/auth/registro',
  '/api/auth/refresco',
  '/api/auth/salida',
];

/**
 * Añade el token de acceso a las peticiones a la API. Si la API responde 401 a una petición que lo
 * llevaba, el token ha caducado: se pide otro con la cookie de refresco y se repite la petición una vez.
 * Si tampoco se puede renovar, la sesión ha terminado y se cierra.
 */
export const autenticacionInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const sesion = inject(Sesion);
  const token = sesion.token();
  if (!token || !peticion.url.startsWith('/api/') || SIN_TOKEN.includes(peticion.url)) {
    return siguiente(peticion);
  }

  return siguiente(conToken(peticion, token)).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== HttpStatusCode.Unauthorized) {
        return throwError(() => error);
      }
      return sesion.renovar().pipe(
        catchError(() => {
          sesion.caducar();
          return throwError(() => error);
        }),
        switchMap((nuevo) => siguiente(conToken(peticion, nuevo))),
      );
    }),
  );
};

function conToken(peticion: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}
