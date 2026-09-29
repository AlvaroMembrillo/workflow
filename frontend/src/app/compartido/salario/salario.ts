import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { LucideInfo } from '@lucide/angular';

import { salarioCompleto, salarioCorto } from '../../core/formato';

/** Salario de una oferta. Siempre en el mismo formato y, si falta, se dice claramente. */
@Component({
  selector: 'app-salario',
  imports: [LucideInfo],
  template: `
    @if (texto(); as salario) {
      <p class="salario">
        <strong>{{ salario }}</strong>
        <span>brutos/año</span>
      </p>
    } @else {
      <p class="salario sin-salario">
        <svg lucideInfo [size]="16"></svg>
        Salario no indicado
      </p>
    }
  `,
  styles: `
    :host {
      display: block;
    }
    .salario {
      display: flex;
      flex-wrap: wrap;
      align-items: baseline;
      gap: var(--space-1) var(--space-2);
      font-variant-numeric: tabular-nums;
    }
    strong {
      font-size: 1.125rem;
      letter-spacing: -0.01em;
    }
    span {
      font-size: 0.875rem;
      color: var(--muted);
    }
    .sin-salario {
      align-items: center;
      font-size: 0.9rem;
      color: var(--muted);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Salario {
  readonly minimo = input.required<number | null>();
  readonly maximo = input.required<number | null>();
  /** Cifras completas (38.000–45.000 €) en lugar del formato corto (38–45 k€). */
  readonly completo = input(false);

  protected readonly texto = computed(() =>
    this.completo()
      ? salarioCompleto(this.minimo(), this.maximo())
      : salarioCorto(this.minimo(), this.maximo()),
  );
}
