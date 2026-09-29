import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { Fecha } from '../fecha/fecha';
import { CandidaturaConFechas, pasosDe } from './pasos';

const DESCRIPCION: Record<string, string> = {
  hecho: 'completado',
  actual: 'paso actual',
  pendiente: 'pendiente',
  saltado: 'no se pasó por este paso',
  aceptada: 'completado',
  rechazada: 'completado',
  retirada: 'completado',
};

/** Línea de progreso de una candidatura: Enviada → En revisión → Decisión, con la fecha de cada paso. */
@Component({
  selector: 'app-progreso-candidatura',
  imports: [Fecha],
  template: `
    <ol aria-label="Progreso de la candidatura">
      @for (paso of pasos(); track paso.nombre) {
        <li [class]="paso.estado" [attr.aria-current]="paso.estado === 'actual' ? 'step' : null">
          <span class="punto" aria-hidden="true"></span>
          <span class="nombre">{{ paso.nombre }}</span>
          <span class="solo-lectores">({{ descripcion[paso.estado] }})</span>
          @if (paso.fecha) {
            <span class="fecha"><app-fecha [iso]="paso.fecha" formato="corta" /></span>
          }
        </li>
      }
    </ol>
  `,
  styles: `
    ol {
      list-style: none;
      margin: 0;
      padding: 0;
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }
    li {
      position: relative;
      display: grid;
      align-content: start;
      gap: 2px;
      padding: 1.5rem var(--space-2) 0 0;
      font-size: 0.85rem;
      color: var(--muted);
      min-width: 0;
    }
    /* Tramo de línea desde este punto hasta el siguiente */
    li::before {
      content: '';
      position: absolute;
      top: 0.45rem;
      left: 0.5rem;
      right: 0;
      height: 2px;
      background: var(--line);
    }
    li:last-child::before {
      display: none;
    }
    .punto {
      position: absolute;
      top: 0;
      left: 0;
      width: 1rem;
      height: 1rem;
      border-radius: 50%;
      background: var(--surface);
      border: 2px solid var(--line);
    }
    .nombre {
      font-weight: 700;
      color: var(--ink);
    }
    .hecho::before {
      background: var(--brand);
    }
    .hecho .punto {
      background: var(--brand);
      border-color: var(--brand);
    }
    .actual .punto {
      border-color: var(--brand);
      box-shadow: 0 0 0 4px var(--brand-soft);
    }
    .pendiente .nombre,
    .saltado .nombre {
      color: var(--muted);
      font-weight: 600;
    }
    .saltado::before {
      background: var(--brand);
    }
    .saltado .punto {
      border-style: dashed;
    }
    .aceptada .punto {
      background: var(--ok-fg);
      border-color: var(--ok-fg);
    }
    .rechazada .punto {
      background: var(--no-fg);
      border-color: var(--no-fg);
    }
    .retirada .punto {
      background: var(--pend-fg);
      border-color: var(--pend-fg);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProgresoCandidatura {
  readonly candidatura = input.required<CandidaturaConFechas>();

  protected readonly pasos = computed(() => pasosDe(this.candidatura()));
  protected readonly descripcion = DESCRIPCION;
}
