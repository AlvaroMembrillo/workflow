import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideCompass } from '@lucide/angular';

@Component({
  selector: 'app-no-encontrada',
  imports: [RouterLink, LucideCompass],
  template: `
    <section>
      <span class="icono"><svg lucideCompass [size]="22"></svg></span>
      <h1>Esta página no existe</h1>
      <p>Puede que el enlace esté mal escrito o que la página se haya movido.</p>
      <a routerLink="/" class="btn btn-primario">Ver ofertas</a>
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
      margin-bottom: var(--space-2);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NoEncontrada {}
