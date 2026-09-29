import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  Injector,
  input,
  OnInit,
  signal,
} from '@angular/core';
import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideCircleAlert, LucideEye, LucideEyeOff } from '@lucide/angular';
import { finalize } from 'rxjs';

import { mensajeDeError, problemaDe } from '../../../core/api/problema';
import { destinoTrasAcceso } from '../../../core/auth/guards';
import { RolRegistrable } from '../../../core/auth/modelos';
import { Sesion } from '../../../core/auth/sesion';
import {
  aplicarErroresDelServidor,
  enfocarPrimerError,
  mensajeDeCampo,
} from '../../../core/formularios';

const MENSAJES = {
  nombre: { required: 'Escribe tu nombre', maxlength: 'Máximo 100 caracteres' },
  email: {
    required: 'Escribe tu email',
    email: 'El email no tiene un formato válido',
    maxlength: 'Máximo 255 caracteres',
    duplicado: 'Ya existe una cuenta con este email.',
  },
  password: {
    required: 'Elige una contraseña',
    minlength: 'La contraseña debe tener al menos 8 caracteres',
    maxlength: 'La contraseña no puede tener más de 72 caracteres',
  },
};

const MENSAJES_NOMBRE_EMPRESA = {
  required: 'Escribe el nombre de la empresa',
  maxlength: 'Máximo 100 caracteres',
};

type Campo = keyof typeof MENSAJES;

@Component({
  selector: 'app-registro',
  imports: [ReactiveFormsModule, RouterLink, LucideCircleAlert, LucideEye, LucideEyeOff],
  templateUrl: './registro.html',
  styleUrls: ['../acceso.css', './registro.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Registro implements OnInit {
  private readonly sesion = inject(Sesion);
  private readonly router = inject(Router);
  private readonly injector = inject(Injector);
  private readonly elemento = inject<ElementRef<HTMLElement>>(ElementRef);

  /** ?tipo=empresa preselecciona la cuenta de empresa (enlace "Para empresas"). */
  readonly tipo = input<string>();
  /** Ruta a la que volver después de registrarse (parámetro ?volver=). */
  readonly volver = input<string>();

  protected readonly enviando = signal(false);
  protected readonly enviado = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly verPassword = signal(false);

  protected readonly formulario = new FormGroup({
    rol: new FormControl<RolRegistrable>('CANDIDATO', { nonNullable: true }),
    nombre: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(100)],
    }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(255)],
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(8), Validators.maxLength(72)],
    }),
  });

  ngOnInit(): void {
    if (this.tipo() === 'empresa') {
      this.formulario.controls.rol.setValue('EMPRESA');
    }
  }

  protected esEmpresa(): boolean {
    return this.formulario.controls.rol.value === 'EMPRESA';
  }

  protected tieneError(campo: Campo): boolean {
    const control = this.formulario.controls[campo];
    return control.invalid && (control.touched || this.enviado());
  }

  protected mensaje(campo: Campo): string | null {
    const mensajes =
      campo === 'nombre' && this.esEmpresa() ? MENSAJES_NOMBRE_EMPRESA : MENSAJES[campo];
    return mensajeDeCampo(this.formulario.controls[campo], mensajes);
  }

  protected emailDuplicado(): boolean {
    return this.formulario.controls.email.hasError('duplicado');
  }

  protected enviar(): void {
    this.enviado.set(true);
    this.error.set(null);
    if (this.formulario.invalid) {
      enfocarPrimerError(this.elemento.nativeElement, this.injector);
      return;
    }

    const { rol, nombre, email, password } = this.formulario.getRawValue();
    this.enviando.set(true);
    this.sesion
      .registrar({ rol, nombre: nombre.trim(), email: email.trim(), password })
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: (usuario) =>
          void this.router.navigateByUrl(destinoTrasAcceso(this.volver(), usuario.rol)),
        error: (error: unknown) => this.mostrarError(error),
      });
  }

  private mostrarError(error: unknown): void {
    if (error instanceof HttpErrorResponse && error.status === HttpStatusCode.Conflict) {
      this.formulario.controls.email.setErrors({ duplicado: true });
    } else if (!aplicarErroresDelServidor(this.formulario, problemaDe(error)?.errores)) {
      this.error.set(mensajeDeError(error));
      return;
    }
    enfocarPrimerError(this.elemento.nativeElement, this.injector);
  }
}
