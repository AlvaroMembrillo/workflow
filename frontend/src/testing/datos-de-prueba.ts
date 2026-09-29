import { Empresa, MiCandidatura, Oferta, Pagina } from '../app/core/api/modelos';

export function ofertaDePrueba(cambios: Partial<Oferta> = {}): Oferta {
  return {
    id: 'oferta-1',
    titulo: 'Desarrollador/a Java Backend',
    descripcion: 'Buscamos a alguien con ganas de aprender.\nTrabajarás con Spring Boot.',
    ubicacion: 'Madrid',
    modalidad: 'REMOTO',
    tipoContrato: 'INDEFINIDO',
    salarioMinimo: 38000,
    salarioMaximo: 45000,
    estado: 'ABIERTA',
    empresa: { id: 'empresa-1', nombre: 'Lumen Seguros' },
    fechaCreacion: '2026-09-27T10:00:00Z',
    fechaActualizacion: '2026-09-27T10:00:00Z',
    ...cambios,
  };
}

export function empresaDePrueba(cambios: Partial<Empresa> = {}): Empresa {
  return {
    id: 'empresa-1',
    nombre: 'Lumen Seguros',
    descripcion: 'Seguros para personas y pymes.',
    sitioWeb: 'https://lumen.example',
    ubicacion: 'Madrid',
    ...cambios,
  };
}

export function candidaturaDePrueba(cambios: Partial<MiCandidatura> = {}): MiCandidatura {
  return {
    id: 'candidatura-1',
    oferta: {
      id: 'oferta-1',
      titulo: 'Desarrollador/a Java Backend',
      estado: 'ABIERTA',
      empresa: { id: 'empresa-1', nombre: 'Lumen Seguros' },
    },
    estado: 'PENDIENTE',
    cartaPresentacion: null,
    fechaCreacion: '2026-09-28T10:00:00Z',
    fechaRevision: null,
    fechaResolucion: null,
    fechaActualizacion: '2026-09-28T10:00:00Z',
    ...cambios,
  };
}

export function paginaDe<T>(elementos: T[], total = elementos.length, tamano = 20): Pagina<T> {
  return {
    content: elementos,
    page: { size: tamano, number: 0, totalElements: total, totalPages: Math.ceil(total / tamano) },
  };
}
