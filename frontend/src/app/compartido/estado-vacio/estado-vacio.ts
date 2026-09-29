import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Explica por qué no hay nada que mostrar. La acción siguiente se pasa como contenido. */
@Component({
  selector: 'app-estado-vacio',
  template: `
    <div class="vacio">
      <p class="titulo">{{ titulo() }}</p>
      @if (texto(); as explicacion) {
        <p class="texto">{{ explicacion }}</p>
      }
      <ng-content />
    </div>
  `,
  styles: `
    .vacio {
      display: grid;
      justify-items: center;
      gap: var(--space-2);
      padding: var(--space-8) var(--space-4);
      text-align: center;
      border: 1px dashed var(--line);
      border-radius: var(--r-card);
      background: var(--surface);
    }
    .titulo {
      font-weight: 700;
      font-size: 1.05rem;
    }
    .texto {
      max-width: 30rem;
      color: var(--muted);
      margin-bottom: var(--space-2);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EstadoVacio {
  readonly titulo = input.required<string>();
  readonly texto = input<string>();
}
