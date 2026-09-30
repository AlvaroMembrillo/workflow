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

/**
 * El entorno de tests no sabe crear direcciones para un fichero en memoria ni descargar al pulsar un
 * enlace. Los tests de descargas comprueban con `vi.spyOn(HTMLAnchorElement.prototype, 'click')` qué
 * se descarga.
 */
URL.createObjectURL = () => 'blob:fichero-de-prueba';
URL.revokeObjectURL = () => undefined;
