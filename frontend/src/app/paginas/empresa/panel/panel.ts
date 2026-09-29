import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { LucideBriefcase, LucidePlus, LucideUsers } from '@lucide/angular';

/** Estructura del panel de empresa: navegación propia y la pantalla elegida a su lado. */
@Component({
  selector: 'app-panel-empresa',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LucideBriefcase, LucideUsers, LucidePlus],
  template: `
    <div class="panel">
      <nav aria-label="Panel de empresa">
        <a
          routerLink="/empresa"
          [class.activo]="enOfertas()"
          [attr.aria-current]="enOfertas() ? 'page' : null"
          ><svg lucideBriefcase [size]="18"></svg>Mis ofertas</a
        >
        <a routerLink="/empresa/perfil" routerLinkActive="activo" ariaCurrentWhenActive="page"
          ><svg lucideUsers [size]="18"></svg>Perfil de empresa</a
        >
        <a routerLink="/empresa/ofertas/nueva" class="btn btn-primario btn-pequeno publicar"
          ><svg lucidePlus [size]="16"></svg>Publicar oferta</a
        >
      </nav>
      <div class="contenido">
        <router-outlet />
      </div>
    </div>
  `,
  styles: `
    .panel {
      display: grid;
      grid-template-columns: 13rem minmax(0, 1fr);
      gap: var(--space-8);
      align-items: start;
    }
    nav {
      position: sticky;
      top: var(--space-4);
      display: grid;
      gap: var(--space-1);
    }
    nav a:not(.btn) {
      display: flex;
      align-items: center;
      gap: var(--space-2);
      padding: var(--space-2) var(--space-3);
      border-radius: var(--r-control);
      color: var(--muted);
      font-weight: 600;
      text-decoration: none;
    }
    nav a:not(.btn):hover {
      background: var(--sunken);
      color: var(--ink);
    }
    nav a.activo {
      background: var(--brand-soft);
      color: var(--brand);
    }
    .publicar {
      margin-top: var(--space-3);
    }
    .contenido {
      min-width: 0;
    }
    @media (max-width: 899px) {
      .panel {
        grid-template-columns: minmax(0, 1fr);
        gap: var(--space-4);
      }
      nav {
        position: static;
        display: flex;
        flex-wrap: wrap;
        align-items: center;
      }
      .publicar {
        margin: 0 0 0 auto;
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Panel {
  private readonly router = inject(Router);
  private readonly url = toSignal(
    this.router.events.pipe(
      filter((evento) => evento instanceof NavigationEnd),
      map((evento) => evento.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  /** "Mis ofertas" agrupa el listado, publicar, editar y las candidaturas de cada oferta. */
  protected readonly enOfertas = computed(() => !this.url().startsWith('/empresa/perfil'));
}
