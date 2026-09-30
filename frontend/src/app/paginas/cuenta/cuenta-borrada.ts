import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideCircleCheck } from '@lucide/angular';

/** Despedida tras borrar la cuenta. */
@Component({
  selector: 'app-cuenta-borrada',
  imports: [RouterLink, LucideCircleCheck],
  template: `
    <section>
      <svg lucideCircleCheck [size]="32"></svg>
      <h1>Hemos borrado tu cuenta</h1>
      <p>
        Ya no guardamos ninguno de tus datos. Te hemos enviado un correo para confirmarlo. Si algún
        día quieres volver, puedes crear una cuenta nueva.
      </p>
      <a routerLink="/" class="btn btn-secundario">Ver ofertas</a>
    </section>
  `,
  styles: `
    section {
      display: grid;
      justify-items: start;
      gap: var(--space-3);
      max-width: 36rem;
      padding-block: var(--space-8);
    }
    svg {
      color: var(--ok-fg);
    }
    p {
      color: var(--muted);
      margin-bottom: var(--space-2);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CuentaBorrada {}
