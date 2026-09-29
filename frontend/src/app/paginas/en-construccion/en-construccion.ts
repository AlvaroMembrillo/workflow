import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { LucideConstruction } from '@lucide/angular';

/**
 * Ocupa las rutas cuyas pantallas todavía no están hechas. El título y el texto vienen de los datos
 * de la ruta, así cada una explica qué habrá en ella.
 */
@Component({
  selector: 'app-en-construccion',
  imports: [LucideConstruction],
  template: `
    <section class="tarjeta">
      <span class="icono"><svg lucideConstruction [size]="22"></svg></span>
      <h1>{{ titulo() }}</h1>
      <p>{{ descripcion() }}</p>
    </section>
  `,
  styles: `
    section {
      display: grid;
      justify-items: start;
      gap: var(--space-3);
      max-width: 40rem;
      padding: var(--space-8);
    }
    .icono {
      display: grid;
      place-items: center;
      width: 2.75rem;
      height: 2.75rem;
      border-radius: 50%;
      background: var(--brand-soft);
      color: var(--brand);
    }
    p {
      color: var(--muted);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EnConstruccion {
  readonly titulo = input.required<string>();
  readonly descripcion = input.required<string>();
}
