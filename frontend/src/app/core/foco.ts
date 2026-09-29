import { afterNextRender, ElementRef, Injector, Signal } from '@angular/core';

/**
 * Lleva el foco a un elemento cuando la vista ya se ha actualizado. Sirve para no perder el foco cuando
 * desaparece el botón que se acaba de pulsar (por ejemplo, al mover una candidatura a otra pestaña):
 * se lleva al mensaje que explica qué ha pasado. El elemento necesita tabindex="-1".
 */
export function enfocarTrasRender(
  elemento: Signal<ElementRef<HTMLElement> | undefined>,
  injector: Injector,
): void {
  afterNextRender(() => elemento()?.nativeElement.focus(), { injector });
}
