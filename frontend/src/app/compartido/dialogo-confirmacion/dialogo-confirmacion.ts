import {
  ChangeDetectionStrategy,
  Component,
  effect,
  ElementRef,
  input,
  output,
  viewChild,
} from '@angular/core';

/**
 * Confirmación para acciones que no se pueden deshacer. Usa <dialog> nativo: atrapa el foco, se cierra
 * con Escape y devuelve el foco al botón que lo abrió. El foco inicial va a "Cancelar", la opción segura.
 */
@Component({
  selector: 'app-dialogo-confirmacion',
  template: `
    <dialog
      #dialogo
      [attr.aria-labelledby]="id() + '-titulo'"
      [attr.aria-describedby]="id() + '-texto'"
      (close)="cancelar.emit()"
    >
      <h2 [id]="id() + '-titulo'">{{ titulo() }}</h2>
      <p [id]="id() + '-texto'">{{ texto() }}</p>
      <div class="acciones">
        <button type="button" class="btn btn-fantasma" autofocus (click)="dialogo.close()">
          Cancelar
        </button>
        <button
          type="button"
          class="btn"
          [class.btn-peligro]="peligro()"
          [class.btn-primario]="!peligro()"
          [disabled]="ocupado()"
          (click)="confirmar.emit()"
        >
          {{ textoConfirmar() }}
        </button>
      </div>
    </dialog>
  `,
  styles: `
    dialog {
      width: min(26rem, calc(100% - 2 * var(--space-4)));
      padding: var(--space-6);
      border: 0;
      border-radius: 16px;
      background: var(--surface);
      color: var(--ink);
      box-shadow: var(--shadow-lg);
    }
    dialog::backdrop {
      background: rgb(0 0 0 / 0.45);
    }
    h2 {
      font-size: 1.15rem;
      margin-bottom: var(--space-2);
    }
    p {
      color: var(--muted);
    }
    .acciones {
      display: flex;
      justify-content: flex-end;
      flex-wrap: wrap;
      gap: var(--space-2);
      margin-top: var(--space-6);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoConfirmacion {
  /** Prefijo para los id del título y el texto. */
  readonly id = input.required<string>();
  readonly abierto = input.required<boolean>();
  readonly titulo = input.required<string>();
  readonly texto = input.required<string>();
  readonly textoConfirmar = input.required<string>();
  /** Acción destructiva: el botón de confirmar va en rojo. */
  readonly peligro = input(false);
  /** Mientras se ejecuta la acción, el botón de confirmar se desactiva. */
  readonly ocupado = input(false);

  readonly confirmar = output<void>();
  /** Se emite al cancelar, pulsar Escape o cerrar el diálogo por cualquier motivo. */
  readonly cancelar = output<void>();

  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');

  constructor() {
    effect(() => {
      const dialogo = this.dialogo().nativeElement;
      if (this.abierto() && !dialogo.open) {
        dialogo.showModal();
      } else if (!this.abierto() && dialogo.open) {
        dialogo.close();
      }
    });
  }
}
