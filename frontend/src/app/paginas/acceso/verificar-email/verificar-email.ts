import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideCircleCheck, LucideUnlink } from '@lucide/angular';
import { finalize } from 'rxjs';

import { mensajeDeError } from '../../../core/api/problema';
import { AccesoApi } from '../../../core/auth/acceso-api';
import { Cuenta } from '../../../core/auth/cuenta';
import { inicioPara } from '../../../core/auth/guards';
import { Sesion } from '../../../core/auth/sesion';
import { tokenDelEnlace } from '../../../core/auth/token-del-enlace';

type Estado = 'comprobando' | 'verificado' | 'enlace-no-valido';

/** Destino del enlace de verificación: confirma el email nada más abrirse. */
@Component({
  selector: 'app-verificar-email',
  imports: [RouterLink, LucideCircleCheck, LucideUnlink],
  templateUrl: './verificar-email.html',
  styleUrl: '../acceso.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VerificarEmail {
  private readonly api = inject(AccesoApi);
  private readonly cuenta = inject(Cuenta);
  protected readonly sesion = inject(Sesion);

  protected readonly estado = signal<Estado>('comprobando');
  protected readonly inicio = inicioPara(this.sesion.rol());
  protected readonly reenviando = signal(false);
  protected readonly reenvio = signal('');

  constructor() {
    const token = tokenDelEnlace();
    if (!token) {
      this.estado.set('enlace-no-valido');
      return;
    }
    this.api.verificarEmail(token).subscribe({
      next: () => {
        this.estado.set('verificado');
        // Quita el aviso de "confirma tu email" de la cabecera
        this.cuenta.recargar();
      },
      error: () => this.estado.set('enlace-no-valido'),
    });
  }

  protected reenviar(): void {
    this.reenviando.set(true);
    this.reenvio.set('');
    this.cuenta
      .reenviarVerificacion()
      .pipe(finalize(() => this.reenviando.set(false)))
      .subscribe({
        next: () => this.reenvio.set('Enviado. Abre el enlace del correo nuevo.'),
        error: (error: unknown) => this.reenvio.set(mensajeDeError(error)),
      });
  }
}
