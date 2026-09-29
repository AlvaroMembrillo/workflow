import { EstadoCandidatura } from '../../core/api/modelos';

/** Datos de una candidatura que necesita la línea de progreso (sirve para la vista del candidato y la de la empresa). */
export interface CandidaturaConFechas {
  estado: EstadoCandidatura;
  fechaCreacion: string;
  fechaRevision: string | null;
  fechaResolucion: string | null;
}

/**
 * - hecho: paso completado
 * - actual: la candidatura está en este paso
 * - pendiente: todavía no ha llegado
 * - saltado: la empresa decidió sin pasar por revisión
 * - aceptada / rechazada / retirada: paso final ya resuelto
 */
export type EstadoPaso =
  'hecho' | 'actual' | 'pendiente' | 'saltado' | 'aceptada' | 'rechazada' | 'retirada';

export interface Paso {
  nombre: string;
  estado: EstadoPaso;
  fecha: string | null;
}

const NOMBRE_FINAL: Record<EstadoCandidatura, string> = {
  PENDIENTE: 'Decisión',
  EN_REVISION: 'Decisión',
  ACEPTADA: 'Seleccionada',
  RECHAZADA: 'No seleccionada',
  RETIRADA: 'Retirada',
};

/** Los tres pasos de una candidatura: enviada, en revisión y decisión (o retirada). */
export function pasosDe(candidatura: CandidaturaConFechas): Paso[] {
  const { estado, fechaCreacion, fechaRevision, fechaResolucion } = candidatura;
  const resuelta = estado === 'ACEPTADA' || estado === 'RECHAZADA' || estado === 'RETIRADA';

  let revision: EstadoPaso;
  if (estado === 'EN_REVISION') {
    revision = 'actual';
  } else if (fechaRevision) {
    revision = 'hecho';
  } else {
    revision = resuelta ? 'saltado' : 'pendiente';
  }

  let final: EstadoPaso = 'pendiente';
  if (estado === 'ACEPTADA') final = 'aceptada';
  if (estado === 'RECHAZADA') final = 'rechazada';
  if (estado === 'RETIRADA') final = 'retirada';

  return [
    {
      nombre: 'Enviada',
      estado: estado === 'PENDIENTE' ? 'actual' : 'hecho',
      fecha: fechaCreacion,
    },
    { nombre: 'En revisión', estado: revision, fecha: fechaRevision },
    { nombre: NOMBRE_FINAL[estado], estado: final, fecha: resuelta ? fechaResolucion : null },
  ];
}
