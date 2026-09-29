import { EstadoCandidatura, EstadoOferta, Modalidad, TipoContrato } from './api/modelos';

/** Textos para mostrar los valores que devuelve la API (ver "Textos" en la guía de diseño). */

export const TEXTO_MODALIDAD: Record<Modalidad, string> = {
  PRESENCIAL: 'Presencial',
  HIBRIDO: 'Híbrido',
  REMOTO: 'En remoto',
};

export const TEXTO_TIPO_CONTRATO: Record<TipoContrato, string> = {
  INDEFINIDO: 'Indefinido',
  TEMPORAL: 'Temporal',
  PRACTICAS: 'Prácticas',
  FREELANCE: 'Autónomo',
};

export const TEXTO_ESTADO_OFERTA: Record<EstadoOferta, string> = {
  ABIERTA: 'Abierta',
  CERRADA: 'Cerrada',
};

/** El candidato ve un lenguaje más cercano: "No seleccionada" en lugar de "Rechazada". */
export const TEXTO_ESTADO_PARA_CANDIDATO: Record<EstadoCandidatura, string> = {
  PENDIENTE: 'Enviada',
  EN_REVISION: 'En revisión',
  ACEPTADA: 'Seleccionada',
  RECHAZADA: 'No seleccionada',
  RETIRADA: 'Retirada',
};

export const TEXTO_ESTADO_PARA_EMPRESA: Record<EstadoCandidatura, string> = {
  PENDIENTE: 'Pendiente',
  EN_REVISION: 'En revisión',
  ACEPTADA: 'Aceptada',
  RECHAZADA: 'Rechazada',
  RETIRADA: 'Retirada',
};
