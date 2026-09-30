import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-pie',
  imports: [RouterLink],
  template: `
    <footer>
      <p>Workflow: ofertas de empleo con el salario a la vista.</p>
      <nav aria-label="Información legal">
        <ul>
          <li><a routerLink="/privacidad">Privacidad</a></li>
          <li><a routerLink="/condiciones">Condiciones de uso</a></li>
          <li><a routerLink="/aviso-legal">Aviso legal</a></li>
          <li><a href="https://github.com/AlvaroMembrillo/workflow">Código en GitHub</a></li>
        </ul>
      </nav>
    </footer>
  `,
  styles: `
    footer {
      max-width: var(--ancho-contenido);
      margin: 0 auto;
      padding: var(--space-8) var(--space-4);
      border-top: 1px solid var(--line);
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      gap: var(--space-3) var(--space-8);
      font-size: 0.875rem;
      color: var(--muted);
    }

    ul {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-wrap: wrap;
      gap: var(--space-2) var(--space-6);
    }

    a {
      display: inline-block;
      padding-block: var(--space-1);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Pie {}
