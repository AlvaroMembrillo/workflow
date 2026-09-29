import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Anterior / Página X de Y / Siguiente. Son enlaces que cambian el parámetro ?pagina= de la URL
 * (empieza en 1), así se puede volver atrás o compartir una página concreta.
 */
@Component({
  selector: 'app-paginacion',
  imports: [RouterLink],
  template: `
    @if (totalPaginas() > 1) {
      <nav aria-label="Paginación">
        @if (pagina() > 1) {
          <a
            class="btn btn-secundario btn-pequeno"
            routerLink="."
            [queryParams]="{ pagina: pagina() === 2 ? null : pagina() - 1 }"
            queryParamsHandling="merge"
            >Anterior</a
          >
        } @else {
          <span class="btn btn-secundario btn-pequeno" aria-disabled="true">Anterior</span>
        }
        <span class="posicion">Página {{ pagina() }} de {{ totalPaginas() }}</span>
        @if (pagina() < totalPaginas()) {
          <a
            class="btn btn-secundario btn-pequeno"
            routerLink="."
            [queryParams]="{ pagina: pagina() + 1 }"
            queryParamsHandling="merge"
            >Siguiente</a
          >
        } @else {
          <span class="btn btn-secundario btn-pequeno" aria-disabled="true">Siguiente</span>
        }
      </nav>
    }
  `,
  styles: `
    nav {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: var(--space-4);
      flex-wrap: wrap;
    }
    .posicion {
      color: var(--muted);
      font-size: 0.9rem;
      font-variant-numeric: tabular-nums;
    }
    [aria-disabled='true'] {
      opacity: 0.5;
      cursor: default;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Paginacion {
  /** Página actual, empezando en 1. */
  readonly pagina = input.required<number>();
  readonly totalPaginas = input.required<number>();
}
