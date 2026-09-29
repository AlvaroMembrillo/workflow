import { httpResource } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  effect,
  ElementRef,
  inject,
  Injector,
  signal,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LucideCircleAlert, LucideCircleCheck, LucideExternalLink } from '@lucide/angular';
import { finalize } from 'rxjs';

import { Empresa } from '../../../core/api/modelos';
import { PanelEmpresaApi } from '../../../core/api/panel-empresa-api';
import { mensajeDeError, problemaDe } from '../../../core/api/problema';
import { ConCambiosSinGuardar } from '../../../core/cambios-sin-guardar';
import {
  aplicarErroresDelServidor,
  enfocarPrimerError,
  mensajeDeCampo,
} from '../../../core/formularios';

const MENSAJES = {
  nombre: { required: 'Escribe el nombre de la empresa', maxlength: 'Máximo 150 caracteres' },
  descripcion: { maxlength: 'Máximo 5000 caracteres' },
  sitioWeb: {
    pattern: 'Escribe la dirección completa, empezando por https://',
    maxlength: 'Máximo 255 caracteres',
  },
  ubicacion: { maxlength: 'Máximo 150 caracteres' },
};

type Campo = keyof typeof MENSAJES;

@Component({
  selector: 'app-perfil-empresa',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    LucideCircleAlert,
    LucideCircleCheck,
    LucideExternalLink,
  ],
  templateUrl: './perfil-empresa.html',
  styleUrl: './perfil-empresa.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PerfilEmpresa implements ConCambiosSinGuardar {
  private readonly api = inject(PanelEmpresaApi);
  private readonly injector = inject(Injector);
  private readonly elemento = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly empresa = httpResource<Empresa>(() => '/api/empresas/me');

  protected readonly formulario = new FormGroup({
    nombre: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(150)],
    }),
    descripcion: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(5000)],
    }),
    sitioWeb: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(/^https?:\/\/\S+$/i), Validators.maxLength(255)],
    }),
    ubicacion: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(150)] }),
  });

  protected readonly enviando = signal(false);
  protected readonly enviado = signal(false);
  protected readonly aviso = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);

  constructor() {
    effect(() => {
      if (this.empresa.hasValue()) {
        const empresa = this.empresa.value();
        this.formulario.reset({
          nombre: empresa.nombre,
          descripcion: empresa.descripcion ?? '',
          sitioWeb: empresa.sitioWeb ?? '',
          ubicacion: empresa.ubicacion ?? '',
        });
      }
    });
  }

  hayCambiosSinGuardar(): boolean {
    return this.formulario.dirty;
  }

  protected tieneError(campo: Campo): boolean {
    const control = this.formulario.controls[campo];
    return control.invalid && (control.touched || this.enviado());
  }

  protected mensaje(campo: Campo): string | null {
    return mensajeDeCampo(this.formulario.controls[campo], MENSAJES[campo]);
  }

  protected guardar(): void {
    this.enviado.set(true);
    this.aviso.set(null);
    this.error.set(null);
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      enfocarPrimerError(this.elemento.nativeElement, this.injector);
      return;
    }

    // Los campos opcionales vacíos se guardan como null, no como texto vacío
    const valores = this.formulario.getRawValue();
    const opcional = (texto: string) => texto.trim() || null;
    this.enviando.set(true);
    this.api
      .actualizarPerfil({
        nombre: valores.nombre.trim(),
        descripcion: opcional(valores.descripcion),
        sitioWeb: opcional(valores.sitioWeb),
        ubicacion: opcional(valores.ubicacion),
      })
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: (empresa) => {
          this.empresa.set(empresa);
          this.aviso.set('Perfil guardado. Así lo ven ahora los candidatos.');
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
