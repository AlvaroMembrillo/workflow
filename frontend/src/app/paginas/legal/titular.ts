import { httpResource } from '@angular/common/http';
import { computed, Injectable } from '@angular/core';

/** Quién responde del portal, tal como lo devuelve GET /api/legal. */
export interface DatosDelTitular {
  titular: string | null;
  nif: string | null;
  domicilio: string | null;
  contacto: string;
}

const SIN_CONFIGURAR = '[pendiente de configurar]';

/**
 * Identidad del titular para las páginas legales. Depende de quién despliegue el portal, así que viene
 * de la configuración del servidor y no está escrita en la web.
 */
@Injectable({ providedIn: 'root' })
export class Titular {
  private readonly datos = httpResource<DatosDelTitular>(() => '/api/legal');

  readonly nombre = computed(() => this.valor('titular'));
  readonly nif = computed(() => this.valor('nif'));
  readonly domicilio = computed(() => this.valor('domicilio'));
  readonly contacto = computed(() => this.valor('contacto'));

  private valor(campo: keyof DatosDelTitular): string {
    return (this.datos.hasValue() ? this.datos.value()[campo] : null) || SIN_CONFIGURAR;
  }
}
