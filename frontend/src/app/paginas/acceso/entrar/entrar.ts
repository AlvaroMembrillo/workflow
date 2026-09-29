import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  Injector,
  input,
  signal,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideCircleAlert, LucideEye, LucideEyeOff, LucideInfo } from '@lucide/angular';
import { finalize } from 'rxjs';

import { mensajeDeError } from '../../../core/api/problema';
import { destinoTrasAcceso } from '../../../core/auth/guards';
import { Sesion } from '../../../core/auth/sesion';
import { enfocarPrimerError, mensajeDeCampo } from '../../../core/formularios';

const MENSAJES = {
  email: { required: 'Escribe tu email', email: 'El email no tiene un formato válido' },
  password: { required: 'Escribe tu contraseña' },
};

@Component({
  selector: 'app-entrar',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    LucideCircleAlert,
    LucideInfo,
    LucideEye,
    LucideEyeOff,
  ],
  templateUrl: './entrar.html',
  styleUrl: '../acceso.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Entrar {
  private readonly sesion = inject(Sesion);
  private readonly router = inject(Router);
  private readonly injector = inject(Injector);
  private readonly elemento = inject<ElementRef<HTMLElement>>(ElementRef);

  /** Ruta a la que volver después de entrar (parámetro ?volver=). */
  readonly volver = input<string>();

  protected readonly caducada = this.sesion.caducada;
  protected readonly enviando = signal(false);
  protected readonly enviado = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly verPassword = signal(false);

  protected readonly formulario = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email],
    }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  protected tieneError(campo: keyof typeof MENSAJES): boolean {
    const control = this.formulario.controls[campo];
    return control.invalid && (control.touched || this.enviado());
  }

  protected mensaje(campo: keyof typeof MENSAJES): string | null {
    return mensajeDeCampo(this.formulario.controls[campo], MENSAJES[campo]);
  }

  protected enviar(): void {
    this.enviado.set(true);
    this.error.set(null);
    if (this.formulario.invalid) {
      enfocarPrimerError(this.elemento.nativeElement, this.injector);
      return;
    }

    this.enviando.set(true);
    this.sesion
      .entrar(this.formulario.getRawValue())
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: (usuario) =>
          void this.router.navigateByUrl(destinoTrasAcceso(this.volver(), usuario.rol)),
        error: (error: unknown) => this.error.set(mensajeDeError(error)),
      });
  }
}
