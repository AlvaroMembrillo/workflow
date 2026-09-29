import { CandidaturaConFechas, pasosDe } from './pasos';

const ENVIO = '2026-09-20T10:00:00Z';
const REVISION = '2026-09-22T10:00:00Z';
const RESOLUCION = '2026-09-25T10:00:00Z';

function candidatura(cambios: Partial<CandidaturaConFechas>): CandidaturaConFechas {
  return {
    estado: 'PENDIENTE',
    fechaCreacion: ENVIO,
    fechaRevision: null,
    fechaResolucion: null,
    ...cambios,
  };
}

describe('pasosDe', () => {
  it('recién enviada: el primer paso es el actual y el resto están pendientes', () => {
    expect(pasosDe(candidatura({}))).toEqual([
      { nombre: 'Enviada', estado: 'actual', fecha: ENVIO },
      { nombre: 'En revisión', estado: 'pendiente', fecha: null },
      { nombre: 'Decisión', estado: 'pendiente', fecha: null },
    ]);
  });

  it('en revisión: marca el segundo paso como actual con su fecha', () => {
    const pasos = pasosDe(candidatura({ estado: 'EN_REVISION', fechaRevision: REVISION }));

    expect(pasos.map((paso) => paso.estado)).toEqual(['hecho', 'actual', 'pendiente']);
    expect(pasos[1].fecha).toBe(REVISION);
  });

  it('aceptada tras revisión: todos los pasos completos y el final dice "Seleccionada"', () => {
    const pasos = pasosDe(
      candidatura({ estado: 'ACEPTADA', fechaRevision: REVISION, fechaResolucion: RESOLUCION }),
    );

    expect(pasos.map((paso) => paso.estado)).toEqual(['hecho', 'hecho', 'aceptada']);
    expect(pasos[2]).toEqual({ nombre: 'Seleccionada', estado: 'aceptada', fecha: RESOLUCION });
  });

  it('rechazada sin pasar por revisión: la revisión aparece como saltada', () => {
    const pasos = pasosDe(candidatura({ estado: 'RECHAZADA', fechaResolucion: RESOLUCION }));

    expect(pasos.map((paso) => paso.estado)).toEqual(['hecho', 'saltado', 'rechazada']);
    expect(pasos[2].nombre).toBe('No seleccionada');
  });

  it('retirada por el candidato', () => {
    const pasos = pasosDe(candidatura({ estado: 'RETIRADA', fechaResolucion: RESOLUCION }));

    expect(pasos[2]).toEqual({ nombre: 'Retirada', estado: 'retirada', fecha: RESOLUCION });
  });
});
