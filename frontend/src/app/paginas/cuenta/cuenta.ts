import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  Injector,
  signal,
  viewChild,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
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
import { Sesion } from '../../core/auth/sesion';
import { guardarFichero } from '../../core/descargas';
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
    RouterLink,
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

  private readonly sesion = inject(Sesion);
  private readonly router = inject(Router);
  private readonly campoNombre = viewChild<ElementRef<HTMLInputElement>>('campoNombre');
  private readonly dialogoBaja = viewChild<ElementRef<HTMLDialogElement>>('dialogoBaja');

  protected readonly editandoNombre = signal(false);
  protected readonly guardandoNombre = signal(false);
  protected readonly avisoNombre = signal('');

  protected readonly descargando = signal(false);
  protected readonly errorPrivacidad = signal<string | null>(null);
  protected readonly borrando = signal(false);
  protected readonly errorBaja = signal<string | null>(null);
  protected readonly errorPasswordBaja = signal<string | null>(null);

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

  protected editarNombre(): void {
    this.avisoNombre.set('');
    this.editandoNombre.set(true);
    afterNextRender(() => this.campoNombre()?.nativeElement.select(), { injector: this.injector });
  }

  protected guardarNombre(evento: Event): void {
    evento.preventDefault();
    const nombre = this.campoNombre()?.nativeElement.value.trim();
    if (!nombre) {
      this.avisoNombre.set('Escribe tu nombre.');
      return;
    }
    this.guardandoNombre.set(true);
    this.cuenta
      .cambiarNombre(nombre)
      .pipe(finalize(() => this.guardandoNombre.set(false)))
      .subscribe({
        next: () => {
          this.editandoNombre.set(false);
          this.avisoNombre.set('Nombre guardado.');
        },
        error: (error: unknown) =>
          this.avisoNombre.set(problemaDe(error)?.errores?.['nombre'] ?? mensajeDeError(error)),
      });
  }

  protected descargarDatos(): void {
    this.descargando.set(true);
    this.errorPrivacidad.set(null);
    this.cuenta
      .descargarDatos()
      .pipe(finalize(() => this.descargando.set(false)))
      .subscribe({
        next: (fichero) => guardarFichero(fichero, 'workflow-mis-datos.json'),
        error: (error: unknown) => this.errorPrivacidad.set(mensajeDeError(error)),
      });
  }

  protected abrirBaja(): void {
    this.errorBaja.set(null);
    this.errorPasswordBaja.set(null);
    this.dialogoBaja()?.nativeElement.showModal();
  }

  protected cerrarBaja(): void {
    // La contraseña no se queda escrita en el diálogo cerrado
    this.dialogoBaja()?.nativeElement.querySelector('form')?.reset();
  }

  protected borrarCuenta(evento: Event): void {
    evento.preventDefault();
    const campo = (evento.target as HTMLFormElement).elements.namedItem(
      'password',
    ) as HTMLInputElement;
    this.errorBaja.set(null);
    this.errorPasswordBaja.set(null);
    if (!campo.value) {
      this.errorPasswordBaja.set('Escribe tu contraseña para confirmar');
      campo.focus();
      return;
    }

    this.borrando.set(true);
    this.cuenta
      .borrar(campo.value)
      .pipe(finalize(() => this.borrando.set(false)))
      .subscribe({
        next: () => {
          this.dialogoBaja()?.nativeElement.close();
          this.sesion.olvidar();
          void this.router.navigateByUrl('/cuenta-borrada');
        },
        error: (error: unknown) => {
          const enPassword = problemaDe(error)?.errores?.['password'];
          if (enPassword) {
            this.errorPasswordBaja.set(enPassword);
            campo.select();
          } else {
            this.errorBaja.set(mensajeDeError(error));
          }
        },
      });
  }

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
