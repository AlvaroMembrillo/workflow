import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { LucideLogOut } from '@lucide/angular';

import { Sesion } from '../../core/auth/sesion';
import { Logo } from '../logo/logo';

@Component({
  selector: 'app-cabecera',
  imports: [RouterLink, RouterLinkActive, Logo, LucideLogOut],
  templateUrl: './cabecera.html',
  styleUrl: './cabecera.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Cabecera {
  protected readonly sesion = inject(Sesion);
  private readonly router = inject(Router);

  protected salir(): void {
    this.sesion.cerrar();
    void this.router.navigateByUrl('/');
  }
}
