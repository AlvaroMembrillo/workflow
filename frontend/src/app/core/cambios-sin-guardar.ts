import { CanDeactivateFn } from '@angular/router';

/** Pantalla con un formulario que se puede abandonar a medias. */
export interface ConCambiosSinGuardar {
  hayCambiosSinGuardar(): boolean;
}

/** Pide confirmación antes de salir de un formulario con cambios sin guardar. */
export const avisarCambiosSinGuardar: CanDeactivateFn<ConCambiosSinGuardar> = (pantalla) =>
  !pantalla.hayCambiosSinGuardar() ||
  window.confirm('Tienes cambios sin guardar. ¿Quieres salir sin guardarlos?');
