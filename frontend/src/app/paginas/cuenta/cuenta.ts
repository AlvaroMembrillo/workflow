import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  Injector,
  signal,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  LucideBadgeCheck,
  LucideCircleAlert,
  LucideCircleCheck,
  LucideEye,
  LucideEyeOff,
  LucideMailWarning,
} from '@lucide/angular';
import { finalize } from 'rxjs';

import { CvCandidato } from '../../compartido/cv-candidato/cv-candidato';
import { mensajeDeError, problemaDe } from '../../core/api/problema';
import { Cuenta as ServicioCuenta } from '../../core/auth/cuenta';
import {
  aplicarErroresDelServidor,
  enfocarPrimerError,
  mensajeDeCampo,
} from '../../core/formularios';

const MENSAJES = {
  passwordActual: { required: 'Escribe tu contraseña actual' },
  passwordNueva: {
    required: 'Elige una contraseña',
    minlength: 'La contraseña debe tener al menos 8 caracteres',
    maxlength: 'La contraseña no puede tener más de 72 caracteres',
  },
};

type Campo = keyof typeof MENSAJES;

/** "Mi cuenta": email y su verificación, avisos por correo y cambio de contraseña. */
@Component({
  selector: 'app-cuenta',
  imports: [
    ReactiveFormsModule,
    CvCandidato,
    LucideBadgeCheck,
    LucideCircleAlert,
    LucideCircleCheck,
    LucideEye,
    LucideEyeOff,
    LucideMailWarning,
  ],
  templateUrl: './cuenta.html',
  styleUrl: './cuenta.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Cuenta {
  protected readonly cuenta = inject(ServicioCuenta);
  private readonly injector = inject(Injector);
  private readonly elemento = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly reenviando = signal(false);
  protected readonly reenvio = signal('');

  protected readonly guardandoAvisos = signal(false);
  protected readonly avisoAvisos = signal('');

  protected readonly enviando = signal(false);
  protected readonly enviado = signal(false);
  protected readonly passwordCambiada = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly verPassword = signal(false);

  protected readonly formulario = new FormGroup({
    passwordActual: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    passwordNueva: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(8), Validators.maxLength(72)],
    }),
  });

  protected reenviar(): void {
    this.reenviando.set(true);
    this.reenvio.set('');
    this.cuenta
      .reenviarVerificacion()
      .pipe(finalize(() => this.reenviando.set(false)))
      .subscribe({
        next: () => this.reenvio.set('Enviado. Si no lo ves, mira en la carpeta de spam.'),
        error: (error: unknown) => this.reenvio.set(mensajeDeError(error)),
      });
  }

  protected cambiarAvisos(evento: Event): void {
    const casilla = evento.target as HTMLInputElement;
    const activados = casilla.checked;
    this.guardandoAvisos.set(true);
    this.avisoAvisos.set('');
    this.cuenta
      .cambiarAvisosPorCorreo(activados)
      .pipe(finalize(() => this.guardandoAvisos.set(false)))
      .subscribe({
        next: () =>
          this.avisoAvisos.set(
            activados ? 'Guardado: recibirás los avisos.' : 'Guardado: no te enviaremos avisos.',
          ),
        error: (error: unknown) => {
          // No se ha guardado: la casilla vuelve a como estaba
          casilla.checked = !activados;
          this.avisoAvisos.set(mensajeDeError(error));
        },
      });
  }

  protected tieneError(campo: Campo): boolean {
    const control = this.formulario.controls[campo];
    return control.invalid && (control.touched || this.enviado());
  }

  protected mensaje(campo: Campo): string | null {
    return mensajeDeCampo(this.formulario.controls[campo], MENSAJES[campo]);
  }

  protected cambiarPassword(): void {
    this.enviado.set(true);
    this.error.set(null);
    this.passwordCambiada.set(false);
    if (this.formulario.invalid) {
      enfocarPrimerError(this.elemento.nativeElement, this.injector);
      return;
    }

    const { passwordActual, passwordNueva } = this.formulario.getRawValue();
    this.enviando.set(true);
    this.cuenta
      .cambiarPassword(passwordActual, passwordNueva)
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: () => {
          this.formulario.reset();
          this.enviado.set(false);
          this.passwordCambiada.set(true);
        },
        error: (error: unknown) => {
          if (aplicarErroresDelServidor(this.formulario, problemaDe(error)?.errores)) {
            enfocarPrimerError(this.elemento.nativeElement, this.injector);
          } else {
            this.error.set(mensajeDeError(error));
          }
        },
      });
  }
}
