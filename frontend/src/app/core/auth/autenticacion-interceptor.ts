import { HttpErrorResponse, HttpInterceptorFn, HttpStatusCode } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { Sesion } from './sesion';

/**
 * Añade el token a las peticiones a la API. Si la API responde 401 a una petición que llevaba token,
 * la sesión ya no es válida (caducada o rechazada) y se cierra.
 */
export const autenticacionInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const sesion = inject(Sesion);
  const token = sesion.token();
  const esApi = peticion.url.startsWith('/api/');

  const conToken =
    token && esApi
      ? peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : peticion;

  return siguiente(conToken).pipe(
    catchError((error: unknown) => {
      if (
        conToken !== peticion &&
        error instanceof HttpErrorResponse &&
        error.status === HttpStatusCode.Unauthorized
      ) {
        sesion.caducar();
      }
      return throwError(() => error);
    }),
  );
};
