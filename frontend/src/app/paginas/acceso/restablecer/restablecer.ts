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
import {
  LucideCircleAlert,
  LucideCircleCheck,
  LucideEye,
  LucideEyeOff,
  LucideUnlink,
} from '@lucide/angular';
import { finalize } from 'rxjs';

import { mensajeDeError, problemaDe } from '../../../core/api/problema';
import { AccesoApi } from '../../../core/auth/acceso-api';
import { Sesion } from '../../../core/auth/sesion';
import { tokenDelEnlace } from '../../../core/auth/token-del-enlace';
import { enfocarTrasRender } from '../../../core/foco';
import {
  aplicarErroresDelServidor,
  enfocarPrimerError,
  mensajeDeCampo,
} from '../../../core/formularios';

const MENSAJES = {
  required: 'Elige una contraseña',
  minlength: 'La contraseña debe tener al menos 8 caracteres',
  maxlength: 'La contraseña no puede tener más de 72 caracteres',
};

type Estado = 'formulario' | 'cambiada' | 'enlace-no-valido';

/** Elige una contraseña nueva con el enlace recibido por correo. */
@Component({
  selector: 'app-restablecer',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    LucideCircleAlert,
    LucideCircleCheck,
    LucideEye,
    LucideEyeOff,
    LucideUnlink,
  ],
  templateUrl: './restablecer.html',
  styleUrl: '../acceso.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Restablecer {
  private readonly api = inject(AccesoApi);
  private readonly injector = inject(Injector);
  private readonly elemento = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly resultado = viewChild<ElementRef<HTMLElement>>('resultado');
  private readonly token = tokenDelEnlace();

  protected readonly sesion = inject(Sesion);
  protected readonly estado = signal<Estado>(this.token ? 'formulario' : 'enlace-no-valido');
  protected readonly enviando = signal(false);
  protected readonly enviado = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly verPassword = signal(false);

  protected readonly formulario = new FormGroup({
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(8), Validators.maxLength(72)],
    }),
  });

  protected tieneError(): boolean {
    const control = this.formulario.controls.password;
    return control.invalid && (control.touched || this.enviado());
  }

  protected mensaje(): string | null {
    return mensajeDeCampo(this.formulario.controls.password, MENSAJES);
  }

  protected enviar(): void {
    this.enviado.set(true);
    this.error.set(null);
    if (this.formulario.invalid || !this.token) {
      enfocarPrimerError(this.elemento.nativeElement, this.injector);
      return;
    }

    this.enviando.set(true);
    this.api
      .restablecerPassword(this.token, this.formulario.controls.password.value)
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: () => this.mostrar('cambiada'),
        error: (error: unknown) => this.mostrarError(error),
      });
  }

  private mostrarError(error: unknown): void {
    const problema = problemaDe(error);
    if (aplicarErroresDelServidor(this.formulario, problema?.errores)) {
      enfocarPrimerError(this.elemento.nativeElement, this.injector);
    } else if (problema?.status === 400) {
      // El enlace ha caducado o ya se ha usado
      this.mostrar('enlace-no-valido');
    } else {
      this.error.set(mensajeDeError(error));
    }
  }

  private mostrar(estado: Estado): void {
    this.estado.set(estado);
    enfocarTrasRender(this.resultado, this.injector);
  }
}
