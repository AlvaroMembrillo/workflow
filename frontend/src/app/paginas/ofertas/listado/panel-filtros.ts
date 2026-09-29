import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { Modalidad, MODALIDADES, TipoContrato, TIPOS_CONTRATO } from '../../../core/api/modelos';
import { TEXTO_MODALIDAD, TEXTO_TIPO_CONTRATO } from '../../../core/textos';

export interface CambioFiltros {
  modalidad?: Modalidad | null;
  tipoContrato?: TipoContrato | null;
}

/**
 * Modalidad y tipo de contrato. La API acepta un único valor por filtro, así que son grupos de
 * opciones (radio) y no casillas. Se aplican al momento, sin botón de "Aplicar".
 */
@Component({
  selector: 'app-panel-filtros',
  template: `
    <fieldset>
      <legend>Modalidad</legend>
      <div class="segmentos">
        <label class="segmento">
          <input
            type="radio"
            [name]="prefijo() + '-modalidad'"
            [checked]="!modalidad()"
            (change)="cambio.emit({ modalidad: null })"
          />
          <span>Todas</span>
        </label>
        @for (opcion of modalidades; track opcion) {
          <label class="segmento">
            <input
              type="radio"
              [name]="prefijo() + '-modalidad'"
              [checked]="modalidad() === opcion"
              (change)="cambio.emit({ modalidad: opcion })"
            />
            <span>{{ textoModalidad[opcion] }}</span>
          </label>
        }
      </div>
    </fieldset>

    <fieldset>
      <legend>Tipo de contrato</legend>
      <label class="opcion">
        <input
          type="radio"
          [name]="prefijo() + '-contrato'"
          [checked]="!tipoContrato()"
          (change)="cambio.emit({ tipoContrato: null })"
        />
        Todos
      </label>
      @for (opcion of tiposContrato; track opcion) {
        <label class="opcion">
          <input
            type="radio"
            [name]="prefijo() + '-contrato'"
            [checked]="tipoContrato() === opcion"
            (change)="cambio.emit({ tipoContrato: opcion })"
          />
          {{ textoContrato[opcion] }}
        </label>
      }
    </fieldset>
  `,
  styles: `
    :host {
      display: grid;
      gap: var(--space-6);
    }
    fieldset {
      border: 0;
      margin: 0;
      padding: 0;
      display: grid;
      gap: var(--space-1);
    }
    legend {
      padding: 0;
      margin-bottom: var(--space-2);
      font-weight: 700;
      font-size: 0.9rem;
    }
    .segmentos {
      display: flex;
      flex-wrap: wrap;
      gap: var(--space-1);
      padding: var(--space-1);
      background: var(--sunken);
      border-radius: 10px;
    }
    .segmento {
      position: relative;
      cursor: pointer;
    }
    .segmento input {
      position: absolute;
      opacity: 0;
      inset: 0;
      margin: 0;
      cursor: pointer;
    }
    .segmento span {
      display: block;
      padding: var(--space-2) var(--space-3);
      border-radius: 7px;
      font-size: 0.875rem;
      font-weight: 600;
      color: var(--muted);
    }
    .segmento:hover span {
      color: var(--ink);
    }
    .segmento:has(input:checked) span {
      background: var(--surface);
      color: var(--ink);
      box-shadow: var(--shadow);
    }
    .segmento:has(input:focus-visible) span {
      outline: 3px solid var(--brand);
      outline-offset: 1px;
    }
    .opcion {
      display: flex;
      align-items: center;
      gap: var(--space-2);
      min-height: 2.25rem;
      font-size: 0.95rem;
      cursor: pointer;
    }
    .opcion input {
      width: 1.05rem;
      height: 1.05rem;
      margin: 0;
      accent-color: var(--brand);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PanelFiltros {
  /** Distingue los grupos de opciones cuando hay dos paneles en la página (lateral y hoja móvil). */
  readonly prefijo = input.required<string>();
  readonly modalidad = input.required<Modalidad | null>();
  readonly tipoContrato = input.required<TipoContrato | null>();
  readonly cambio = output<CambioFiltros>();

  protected readonly modalidades = MODALIDADES;
  protected readonly tiposContrato = TIPOS_CONTRATO;
  protected readonly textoModalidad = TEXTO_MODALIDAD;
  protected readonly textoContrato = TEXTO_TIPO_CONTRATO;
}
