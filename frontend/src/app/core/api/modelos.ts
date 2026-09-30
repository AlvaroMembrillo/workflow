/** Tipos de las respuestas de la API. Las fechas llegan como texto ISO 8601. */

export type Modalidad = 'PRESENCIAL' | 'HIBRIDO' | 'REMOTO';
export type TipoContrato = 'INDEFINIDO' | 'TEMPORAL' | 'PRACTICAS' | 'FREELANCE';
export type EstadoOferta = 'ABIERTA' | 'CERRADA';
export type EstadoCandidatura = 'PENDIENTE' | 'EN_REVISION' | 'ACEPTADA' | 'RECHAZADA' | 'RETIRADA';

export const MODALIDADES: readonly Modalidad[] = ['PRESENCIAL', 'HIBRIDO', 'REMOTO'];
export const TIPOS_CONTRATO: readonly TipoContrato[] = [
  'INDEFINIDO',
  'TEMPORAL',
  'PRACTICAS',
  'FREELANCE',
];

/** Página de resultados tal como la devuelve Spring Data (formato VIA_DTO). */
export interface Pagina<T> {
  content: T[];
  page: {
    size: number;
    /** Empieza en 0. */
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

export interface EmpresaResumen {
  id: string;
  nombre: string;
}

export interface Empresa {
  id: string;
  nombre: string;
  descripcion: string | null;
  sitioWeb: string | null;
  ubicacion: string | null;
}

export interface Oferta {
  id: string;
  titulo: string;
  descripcion: string;
  ubicacion: string;
  modalidad: Modalidad;
  tipoContrato: TipoContrato;
  /** Bruto anual en euros. */
  salarioMinimo: number | null;
  salarioMaximo: number | null;
  estado: EstadoOferta;
  empresa: EmpresaResumen;
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface OfertaResumen {
  id: string;
  titulo: string;
  estado: EstadoOferta;
  empresa: EmpresaResumen;
}

export interface MiCandidatura {
  id: string;
  oferta: OfertaResumen;
  estado: EstadoCandidatura;
  cartaPresentacion: string | null;
  fechaCreacion: string;
  fechaRevision: string | null;
  fechaResolucion: string | null;
  fechaActualizacion: string;
}

/** Cuántas candidaturas tiene una oferta en cada estado. */
export interface ResumenCandidaturas {
  total: number;
  pendientes: number;
  enRevision: number;
  aceptadas: number;
  rechazadas: number;
  retiradas: number;
}

/** Una oferta de la empresa en su panel, con el resumen de sus candidaturas. */
export interface OfertaConCandidaturas {
  oferta: Oferta;
  candidaturas: ResumenCandidaturas;
}

export interface CandidatoResumen {
  id: string;
  nombre: string;
  email: string;
}

/** El currículum en PDF de un candidato, sin su contenido. */
export interface Curriculum {
  nombreFichero: string;
  /** En bytes. */
  tamano: number;
  fechaSubida: string;
}

/** Una candidatura vista por la empresa que publicó la oferta. */
export interface CandidaturaRecibida {
  id: string;
  candidato: CandidatoResumen;
  estado: EstadoCandidatura;
  cartaPresentacion: string | null;
  /** El currículum del candidato, o null si no ha subido ninguno. */
  cv: Curriculum | null;
  fechaCreacion: string;
  fechaRevision: string | null;
  fechaResolucion: string | null;
  fechaActualizacion: string;
}

export interface OfertaRequest {
  titulo: string;
  descripcion: string;
  ubicacion: string;
  modalidad: Modalidad;
  tipoContrato: TipoContrato;
  salarioMinimo: number;
  salarioMaximo: number;
}

export interface EmpresaRequest {
  nombre: string;
  descripcion: string | null;
  sitioWeb: string | null;
  ubicacion: string | null;
}
