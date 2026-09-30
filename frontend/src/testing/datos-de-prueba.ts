import {
  Curriculum,
  Denuncia,
  Empresa,
  MiCandidatura,
  Oferta,
  Pagina,
} from '../app/core/api/modelos';
import { Usuario } from '../app/core/auth/modelos';

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

export function usuarioDePrueba(cambios: Partial<Usuario> = {}): Usuario {
  return {
    id: '8f1c2a4e-0000-4000-8000-000000000001',
    email: 'ana@test.com',
    nombre: 'Ana García',
    rol: 'CANDIDATO',
    fechaCreacion: '2026-09-20T10:00:00Z',
    emailVerificado: true,
    avisosPorCorreo: true,
    ...cambios,
  };
}

export function curriculumDePrueba(cambios: Partial<Curriculum> = {}): Curriculum {
  return {
    nombreFichero: 'CV Ana García.pdf',
    tamano: 182_400,
    fechaSubida: '2026-09-28T10:00:00Z',
    ...cambios,
  };
}

export function denunciaDePrueba(cambios: Partial<Denuncia> = {}): Denuncia {
  return {
    id: 'denuncia-1',
    motivo: 'FRAUDE',
    detalle: 'Piden 200 € para el material.',
    estado: 'PENDIENTE',
    fechaCreacion: '2026-09-29T10:00:00Z',
    fechaResolucion: null,
    emailDenunciante: 'ana@test.com',
    oferta: {
      id: 'oferta-1',
      titulo: 'Gana 3000 € desde casa',
      descripcion: 'Trabajo fácil sin experiencia.',
      estado: 'ABIERTA',
      empresaId: 'empresa-1',
      empresaNombre: 'Dinero Fácil',
      empresaEmail: 'hola@dinerofacil.test',
      empresaSuspendida: false,
    },
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
