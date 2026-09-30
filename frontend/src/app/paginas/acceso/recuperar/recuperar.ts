import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  Injector,
  signal,
  viewChild,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LucideCircleAlert, LucideMailCheck } from '@lucide/angular';
import { finalize } from 'rxjs';

import { mensajeDeError } from '../../../core/api/problema';
import { AccesoApi } from '../../../core/auth/acceso-api';
import { enfocarTrasRender } from '../../../core/foco';
import { enfocarPrimerError, mensajeDeCampo } from '../../../core/formularios';

const MENSAJES = { required: 'Escribe tu email', email: 'El email no tiene un formato válido' };

/** Pide un enlace para cambiar la contraseña olvidada. */
@Component({
  selector: 'app-recuperar',
  imports: [ReactiveFormsModule, RouterLink, LucideCircleAlert, LucideMailCheck],
  templateUrl: './recuperar.html',
  styleUrl: '../acceso.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Recuperar {
  private readonly api = inject(AccesoApi);
  private readonly injector = inject(Injector);
  private readonly elemento = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly confirmacion = viewChild<ElementRef<HTMLElement>>('confirmacion');

  protected readonly enviando = signal(false);
  protected readonly enviado = signal(false);
  protected readonly error = signal<string | null>(null);
  /** Email al que se ha enviado el enlace; null mientras no se haya pedido. */
  protected readonly enviadoA = signal<string | null>(null);

  protected readonly formulario = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email],
    }),
  });

  protected tieneError(): boolean {
    const control = this.formulario.controls.email;
    return control.invalid && (control.touched || this.enviado());
  }

  protected mensaje(): string | null {
    return mensajeDeCampo(this.formulario.controls.email, MENSAJES);
  }

  protected enviar(): void {
    this.enviado.set(true);
    this.error.set(null);
    if (this.formulario.invalid) {
      enfocarPrimerError(this.elemento.nativeElement, this.injector);
      return;
    }

    const email = this.formulario.controls.email.value.trim();
    this.enviando.set(true);
    this.api
      .pedirCambioDePassword(email)
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: () => {
          this.enviadoA.set(email);
          // El formulario desaparece: el foco va al mensaje que lo sustituye
          enfocarTrasRender(this.confirmacion, this.injector);
        },
        error: (error: unknown) => this.error.set(mensajeDeError(error)),
      });
  }
}
