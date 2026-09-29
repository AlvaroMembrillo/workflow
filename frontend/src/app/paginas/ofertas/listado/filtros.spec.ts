import { hayFiltros, leerModalidad, leerPagina, leerTipoContrato, parametrosApi } from './filtros';

describe('filtros del buscador', () => {
  it('ignora los valores de la URL que no son válidos', () => {
    expect(leerModalidad('REMOTO')).toBe('REMOTO');
    expect(leerModalidad('A_VECES')).toBeNull();
    expect(leerTipoContrato('practicas')).toBeNull();
    expect(leerPagina('3')).toBe(3);
    expect(leerPagina('0')).toBe(1);
    expect(leerPagina('dos')).toBe(1);
    expect(leerPagina(undefined)).toBe(1);
  });

  it('convierte los filtros en parámetros de la API, que cuenta las páginas desde 0', () => {
    const parametros = parametrosApi(
      { texto: 'java', ubicacion: '', modalidad: 'REMOTO', tipoContrato: null },
      2,
    );

    expect(parametros).toEqual({ page: 1, size: 20, texto: 'java', modalidad: 'REMOTO' });
  });

  it('sabe si hay algún filtro aplicado', () => {
    const vacio = { texto: '', ubicacion: '', modalidad: null, tipoContrato: null };
    expect(hayFiltros(vacio)).toBe(false);
    expect(hayFiltros({ ...vacio, tipoContrato: 'TEMPORAL' })).toBe(true);
  });
});
