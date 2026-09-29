import { Modalidad, MODALIDADES, TipoContrato, TIPOS_CONTRATO } from '../../../core/api/modelos';

/** Filtros del buscador. Viven en la URL para poder compartir la búsqueda o volver atrás. */
export interface FiltroOfertas {
  texto: string;
  ubicacion: string;
  modalidad: Modalidad | null;
  tipoContrato: TipoContrato | null;
}

export const TAMANO_PAGINA = 20;

// Los valores de la URL los escribe cualquiera: lo que no sea válido se ignora
export const leerTexto = (valor: string | undefined): string => (valor ?? '').trim();

export const leerModalidad = (valor: string | undefined): Modalidad | null =>
  MODALIDADES.includes(valor as Modalidad) ? (valor as Modalidad) : null;

export const leerTipoContrato = (valor: string | undefined): TipoContrato | null =>
  TIPOS_CONTRATO.includes(valor as TipoContrato) ? (valor as TipoContrato) : null;

export const leerPagina = (valor: string | undefined): number => {
  const pagina = Number(valor);
  return Number.isInteger(pagina) && pagina > 0 ? pagina : 1;
};

export function hayFiltros(filtro: FiltroOfertas): boolean {
  return !!(filtro.texto || filtro.ubicacion || filtro.modalidad || filtro.tipoContrato);
}

/** Parámetros para GET /api/ofertas. La API cuenta las páginas desde 0. */
export function parametrosApi(
  filtro: FiltroOfertas,
  pagina: number,
): Record<string, string | number> {
  const parametros: Record<string, string | number> = { page: pagina - 1, size: TAMANO_PAGINA };
  if (filtro.texto) parametros['texto'] = filtro.texto;
  if (filtro.ubicacion) parametros['ubicacion'] = filtro.ubicacion;
  if (filtro.modalidad) parametros['modalidad'] = filtro.modalidad;
  if (filtro.tipoContrato) parametros['tipoContrato'] = filtro.tipoContrato;
  return parametros;
}
