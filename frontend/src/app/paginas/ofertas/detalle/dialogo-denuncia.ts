import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  effect,
  ElementRef,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideCircleAlert, LucideCircleCheck } from '@lucide/angular';
import { finalize } from 'rxjs';

import { ModeracionApi } from '../../../core/api/moderacion-api';
import { MotivoDenuncia, MOTIVOS_DENUNCIA } from '../../../core/api/modelos';
import { mensajeDeError } from '../../../core/api/problema';
import { TEXTO_MOTIVO_DENUNCIA } from '../../../core/textos';

export const LONGITUD_MAXIMA_DETALLE = 1000;

/**
 * Formulario para denunciar una oferta, en un <dialog> nativo: atrapa el foco, se cierra con Escape y
 * devuelve el foco al botón que lo abrió.
 */
@Component({
  selector: 'app-dialogo-denuncia',
  imports: [FormsModule, LucideCircleAlert, LucideCircleCheck],
  templateUrl: './dialogo-denuncia.html',
  styleUrl: './dialogo-denuncia.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoDenuncia {
  private readonly api = inject(ModeracionApi);
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');

  readonly ofertaId = input.required<string>();
  readonly abierto = input.required<boolean>();
  /** Se emite al cerrar el diálogo por cualquier motivo. */
  readonly cerrar = output<void>();

  protected readonly motivos = MOTIVOS_DENUNCIA;
  protected readonly textoMotivo = TEXTO_MOTIVO_DENUNCIA;
  protected readonly longitudMaxima = LONGITUD_MAXIMA_DETALLE;

  protected readonly motivo = signal<MotivoDenuncia | null>(null);
  protected readonly detalle = signal('');
  protected readonly enviando = signal(false);
  protected readonly intentado = signal(false);
  protected readonly enviada = signal(false);
  protected readonly error = signal<string | null>(null);

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

  protected enviar(evento: Event): void {
    evento.preventDefault();
    this.intentado.set(true);
    this.error.set(null);
    const motivo = this.motivo();
    if (!motivo) {
      return;
    }

    this.enviando.set(true);
    this.api
      .denunciar(this.ofertaId(), motivo, this.detalle().trim() || null)
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: () => this.enviada.set(true),
        error: (fallo: unknown) => {
          // 409: ya la había denunciado. Para el usuario es lo mismo que haberla enviado
          if (fallo instanceof HttpErrorResponse && fallo.status === HttpStatusCode.Conflict) {
            this.enviada.set(true);
          } else {
            this.error.set(mensajeDeError(fallo));
          }
        },
      });
  }
}
