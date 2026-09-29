import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-pie',
  template: `
    <footer>
      <p>
        Workflow: ofertas de empleo con el salario a la vista.
        <a href="https://github.com/AlvaroMembrillo/workflow">Código en GitHub</a>
      </p>
    </footer>
  `,
  styles: `
    footer {
      max-width: var(--ancho-contenido);
      margin: 0 auto;
      padding: var(--space-8) var(--space-4);
      border-top: 1px solid var(--line);
      font-size: 0.875rem;
      color: var(--muted);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Pie {}
