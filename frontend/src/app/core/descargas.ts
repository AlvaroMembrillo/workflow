/**
 * Guarda en el dispositivo un fichero que se ha pedido a la API. No sirve un enlace normal porque la
 * API necesita el token en una cabecera, y el navegador no la envía al seguir un enlace.
 */
export function guardarFichero(contenido: Blob, nombre: string): void {
  const url = URL.createObjectURL(contenido);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombre;
  document.body.append(enlace);
  enlace.click();
  enlace.remove();
  URL.revokeObjectURL(url);
}
