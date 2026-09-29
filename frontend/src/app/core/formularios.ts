import { afterNextRender, Injector } from '@angular/core';
import { AbstractControl, FormGroup } from '@angular/forms';

/**
 * Primer mensaje de error de un control. Los errores que vienen del servidor (clave "servidor")
 * tienen prioridad; el resto se busca en los mensajes de la pantalla por nombre de validador.
 */
export function mensajeDeCampo(
  control: AbstractControl,
  mensajes: Record<string, string>,
): string | null {
  const errores = control.errors;
  if (!errores) {
    return null;
  }
  if (typeof errores['servidor'] === 'string') {
    return errores['servidor'];
  }
  const clave = Object.keys(errores).find((nombre) => nombre in mensajes);
  return clave ? mensajes[clave] : null;
}

/**
 * Marca en cada campo el error que ha devuelto la API (respuesta 400 con "errores").
 * Devuelve true si alguno de los errores corresponde a un campo del formulario.
 */
export function aplicarErroresDelServidor(
  formulario: FormGroup,
  errores: Record<string, string> | undefined,
): boolean {
  let alguno = false;
  for (const [campo, mensaje] of Object.entries(errores ?? {})) {
    const control = formulario.get(campo);
    if (control) {
      control.setErrors({ ...control.errors, servidor: mensaje });
      control.markAsTouched();
      alguno = true;
    }
  }
  return alguno;
}

/** Lleva el foco al primer campo con error, cuando la vista ya muestra los errores. */
export function enfocarPrimerError(contenedor: HTMLElement, injector: Injector): void {
  afterNextRender(() => contenedor.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus(), {
    injector,
  });
}
