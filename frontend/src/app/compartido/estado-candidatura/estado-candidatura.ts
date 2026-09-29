import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import {
  LucideArrowRight,
  LucideCircleCheck,
  LucideCircleX,
  LucideClock,
  LucideUndo2,
} from '@lucide/angular';

import { EstadoCandidatura as Estado } from '../../core/api/modelos';
import { TEXTO_ESTADO_PARA_CANDIDATO, TEXTO_ESTADO_PARA_EMPRESA } from '../../core/textos';

/**
 * Insignia con el estado de una candidatura. Siempre lleva texto e icono: el color nunca es la única pista.
 * El candidato y la empresa ven textos distintos ("No seleccionada" frente a "Rechazada").
 */
@Component({
  selector: 'app-estado-candidatura',
  imports: [LucideArrowRight, LucideClock, LucideCircleCheck, LucideCircleX, LucideUndo2],
  template: `
    <span [class]="'insignia ' + tono()">
      @switch (estado()) {
        @case ('PENDIENTE') {
          <svg lucideArrowRight [size]="14"></svg>
        }
        @case ('EN_REVISION') {
          <svg lucideClock [size]="14"></svg>
        }
        @case ('ACEPTADA') {
          <svg lucideCircleCheck [size]="14"></svg>
        }
        @case ('RECHAZADA') {
          <svg lucideCircleX [size]="14"></svg>
        }
        @case ('RETIRADA') {
          <svg lucideUndo2 [size]="14"></svg>
        }
      }
      {{ texto() }}
    </span>
  `,
  styles: `
    .insignia {
      display: inline-flex;
      align-items: center;
      gap: var(--space-1);
      min-height: 1.6rem;
      padding: 0 var(--space-2);
      border-radius: var(--r-pill);
      font-size: 0.8rem;
      font-weight: 700;
      white-space: nowrap;
    }
    .pendiente {
      background: var(--pend-bg);
      color: var(--pend-fg);
    }
    .revision {
      background: var(--rev-bg);
      color: var(--rev-fg);
    }
    .aceptada {
      background: var(--ok-bg);
      color: var(--ok-fg);
    }
    .rechazada {
      background: var(--no-bg);
      color: var(--no-fg);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EstadoCandidatura {
  readonly estado = input.required<Estado>();
  readonly vista = input<'candidato' | 'empresa'>('candidato');

  protected readonly texto = computed(() =>
    this.vista() === 'candidato'
      ? TEXTO_ESTADO_PARA_CANDIDATO[this.estado()]
      : TEXTO_ESTADO_PARA_EMPRESA[this.estado()],
  );

  protected readonly tono = computed(() => {
    switch (this.estado()) {
      case 'EN_REVISION':
        return 'revision';
      case 'ACEPTADA':
        return 'aceptada';
      case 'RECHAZADA':
        return 'rechazada';
      default:
        return 'pendiente';
    }
  });
}
