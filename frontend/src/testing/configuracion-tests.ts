/**
 * Preparación común de los tests. jsdom todavía no implementa los métodos de <dialog>, que la aplicación
 * usa para la hoja de filtros y las confirmaciones: aquí se simulan con el comportamiento mínimo.
 */
if (!HTMLDialogElement.prototype.showModal) {
  HTMLDialogElement.prototype.showModal = function (this: HTMLDialogElement) {
    this.open = true;
  };
  HTMLDialogElement.prototype.close = function (this: HTMLDialogElement, valor?: string) {
    if (!this.open) {
      return;
    }
    this.open = false;
    this.returnValue = valor ?? '';
    this.dispatchEvent(new Event('close'));
  };
}
