/** Número de página del parámetro ?pagina= de la URL (empieza en 1). Si no es válido, la primera. */
export const leerPagina = (valor: string | undefined): number => {
  const pagina = Number(valor);
  return Number.isInteger(pagina) && pagina > 0 ? pagina : 1;
};
