import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { LucideMailWarning } from '@lucide/angular';
import { finalize } from 'rxjs';

import { mensajeDeError } from '../../core/api/problema';
import { Cuenta } from '../../core/auth/cuenta';

/** Banda bajo la cabecera que recuerda confirmar el email y permite pedir otro enlace. */
@Component({
  selector: 'app-aviso-verificacion',
  imports: [LucideMailWarning],
  template: `
    @if (cuenta.emailSinVerificar()) {
      <aside class="banda" aria-label="Email sin confirmar">
        <div class="contenedor">
          <svg lucideMailWarning [size]="20"></svg>
          <p>
            <strong>Confirma tu email.</strong>
            Te hemos enviado un enlace a {{ cuenta.datos.value()?.email }}.
            <span role="status">{{ resultado() }}</span>
          </p>
          <button
            type="button"
            class="btn btn-secundario btn-pequeno"
            [disabled]="enviando()"
            (click)="reenviar()"
          >
            {{ enviando() ? 'Enviando…' : 'Enviar otro enlace' }}
          </button>
        </div>
      </aside>
    }
  `,
  styles: `
    .banda {
      background: var(--rev-bg);
      color: var(--rev-fg);
      border-bottom: 1px solid var(--line);
    }

    .contenedor {
      max-width: var(--ancho-contenido);
      margin: 0 auto;
      padding: var(--space-2) var(--space-4);
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: var(--space-2) var(--space-3);
      font-size: 0.95rem;
    }

    svg {
      flex: none;
    }

    p {
      flex: 1 1 16rem;
      overflow-wrap: anywhere;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AvisoVerificacion {
  protected readonly cuenta = inject(Cuenta);

  protected readonly enviando = signal(false);
  protected readonly resultado = signal('');

  protected reenviar(): void {
    this.enviando.set(true);
    this.resultado.set('');
    this.cuenta
      .reenviarVerificacion()
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: () => this.resultado.set('Enviado. Si no lo ves, mira en la carpeta de spam.'),
        error: (error: unknown) => this.resultado.set(mensajeDeError(error)),
      });
  }
}
