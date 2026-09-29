import { HttpErrorResponse } from '@angular/common/http';

/** Respuesta de error de la API en formato Problem Details (RFC 9457). */
export interface Problema {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  /** Solo en errores de validación (400): mensaje de cada campo. */
  errores?: Record<string, string>;
}

/** Devuelve el Problem Details de un error HTTP, o null si la respuesta no lo trae. */
export function problemaDe(error: unknown): Problema | null {
  if (
    error instanceof HttpErrorResponse &&
    typeof error.error === 'object' &&
    error.error !== null
  ) {
    return error.error as Problema;
  }
  return null;
}

/**
 * Mensaje para mostrar al usuario cuando una petición falla y la pantalla no trata ese error en concreto.
 * Usa el detalle que envía la API y, si no hay, explica qué hacer.
 */
export function mensajeDeError(error: unknown): string {
  if (error instanceof HttpErrorResponse && error.status === 0) {
    return 'No hemos podido conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo.';
  }
  if (error instanceof HttpErrorResponse && error.status >= 500) {
    return 'Algo ha fallado en el servidor. Inténtalo de nuevo en unos minutos.';
  }
  return problemaDe(error)?.detail ?? 'No hemos podido completar la operación. Inténtalo de nuevo.';
}
