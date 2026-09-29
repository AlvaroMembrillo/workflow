import { HttpErrorResponse, HttpStatusCode, httpResource } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  Injector,
  input,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideCircleAlert } from '@lucide/angular';
import { finalize, Observable } from 'rxjs';

import { TarjetaOferta } from '../../../compartido/tarjeta-oferta/tarjeta-oferta';
import {
  Empresa,
  Modalidad,
  MODALIDADES,
  Oferta,
  OfertaConCandidaturas,
  OfertaRequest,
  TipoContrato,
  TIPOS_CONTRATO,
} from '../../../core/api/modelos';
import { PanelEmpresaApi } from '../../../core/api/panel-empresa-api';
import { mensajeDeError, problemaDe } from '../../../core/api/problema';
import { ConCambiosSinGuardar } from '../../../core/cambios-sin-guardar';
import {
  aplicarErroresDelServidor,
  enfocarPrimerError,
  mensajeDeCampo,
} from '../../../core/formularios';
import { TEXTO_MODALIDAD, TEXTO_TIPO_CONTRATO } from '../../../core/textos';

const MAXIMO_DESCRIPCION = 10000;

const MENSAJES = {
  titulo: { required: 'Escribe el título del puesto', maxlength: 'Máximo 150 caracteres' },
  descripcion: {
    required: 'Describe el puesto: qué hará la persona y qué buscáis',
    maxlength: `Máximo ${MAXIMO_DESCRIPCION} caracteres`,
  },
  ubicacion: { required: 'Indica dónde está el puesto', maxlength: 'Máximo 150 caracteres' },
  modalidad: { required: 'Elige la modalidad' },
  tipoContrato: { required: 'Elige el tipo de contrato' },
  salarioMinimo: {
    required: 'El salario mínimo es obligatorio',
    min: 'Debe ser mayor que cero',
  },
  salarioMaximo: {
    required: 'El salario máximo es obligatorio',
    min: 'Debe ser mayor que cero',
  },
};

type Campo = keyof typeof MENSAJES;

/** El mínimo no puede superar al máximo. El error se asocia al grupo de salario. */
function rangoSalarialValido(grupo: AbstractControl): ValidationErrors | null {
  const minimo = grupo.get('salarioMinimo')?.value as number | null;
  const maximo = grupo.get('salarioMaximo')?.value as number | null;
  return minimo !== null && maximo !== null && minimo > maximo ? { rangoSalarial: true } : null;
}

/**
 * Publicar una oferta nueva (/empresa/ofertas/nueva) o editar una existente (/empresa/ofertas/:id/editar).
 * Muestra a la derecha cómo verán la tarjeta los candidatos mientras se escribe.
 */
@Component({
  selector: 'app-formulario-oferta',
  imports: [ReactiveFormsModule, RouterLink, TarjetaOferta, LucideCircleAlert],
  templateUrl: './formulario-oferta.html',
  styleUrl: './formulario-oferta.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormularioOferta implements ConCambiosSinGuardar {
  private readonly api = inject(PanelEmpresaApi);
  private readonly router = inject(Router);
  private readonly injector = inject(Injector);
  private readonly elemento = inject<ElementRef<HTMLElement>>(ElementRef);

  /** Id de la oferta al editar; sin id, se publica una nueva. */
  readonly id = input<string>();

  protected readonly editando = computed(() => !!this.id());

  /** Al editar, la oferta actual (con la comprobación de que es de esta empresa). */
  protected readonly existente = httpResource<OfertaConCandidaturas>(() =>
    this.id() ? `/api/empresas/me/ofertas/${this.id()}` : undefined,
  );
  /** Al publicar, el perfil de la empresa para mostrar su nombre en la vista previa. */
  private readonly miEmpresa = httpResource<Empresa>(() =>
    this.id() ? undefined : '/api/empresas/me',
  );
  protected readonly esDeOtraEmpresa = computed(
    () =>
      (this.existente.error() as HttpErrorResponse | undefined)?.status ===
      HttpStatusCode.Forbidden,
  );

  protected readonly formulario = new FormGroup(
    {
      titulo: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required, Validators.maxLength(150)],
      }),
      descripcion: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required, Validators.maxLength(MAXIMO_DESCRIPCION)],
      }),
      ubicacion: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required, Validators.maxLength(150)],
      }),
      modalidad: new FormControl<Modalidad | null>(null, Validators.required),
      tipoContrato: new FormControl<TipoContrato | null>(null, Validators.required),
      salarioMinimo: new FormControl<number | null>(null, [Validators.required, Validators.min(1)]),
      salarioMaximo: new FormControl<number | null>(null, [Validators.required, Validators.min(1)]),
    },
    { validators: rangoSalarialValido },
  );

  private readonly valores = toSignal(this.formulario.valueChanges, {
    initialValue: this.formulario.getRawValue(),
  });

  /** Tarjeta de ejemplo con lo que se está escribiendo. */
  protected readonly vistaPrevia = computed<Oferta>(() => {
    const valores = this.valores();
    const ahora = new Date().toISOString();
    return {
      id: 'vista-previa',
      titulo: valores.titulo?.trim() || 'Título del puesto',
      descripcion: valores.descripcion ?? '',
      ubicacion: valores.ubicacion?.trim() || 'Ubicación',
      modalidad: valores.modalidad ?? 'PRESENCIAL',
      tipoContrato: valores.tipoContrato ?? 'INDEFINIDO',
      salarioMinimo: valores.salarioMinimo ?? null,
      salarioMaximo: valores.salarioMaximo ?? null,
      estado: 'ABIERTA',
      empresa: this.existente.hasValue()
        ? this.existente.value().oferta.empresa
        : {
            id: '',
            nombre: this.miEmpresa.hasValue() ? this.miEmpresa.value().nombre : 'Tu empresa',
          },
      fechaCreacion: this.existente.hasValue()
        ? this.existente.value().oferta.fechaCreacion
        : ahora,
      fechaActualizacion: ahora,
    };
  });

  protected readonly enviando = signal(false);
  protected readonly enviado = signal(false);
  protected readonly error = signal<string | null>(null);
  private guardado = false;

  protected readonly modalidades = MODALIDADES;
  protected readonly tiposContrato = TIPOS_CONTRATO;
  protected readonly textoModalidad = TEXTO_MODALIDAD;
  protected readonly textoContrato = TEXTO_TIPO_CONTRATO;
  protected readonly maximoDescripcion = MAXIMO_DESCRIPCION;

  constructor() {
    // Al editar, el formulario se rellena con la oferta cuando llega
    effect(() => {
      if (this.existente.hasValue()) {
        const { oferta } = this.existente.value();
        this.formulario.reset({
          titulo: oferta.titulo,
          descripcion: oferta.descripcion,
          ubicacion: oferta.ubicacion,
          modalidad: oferta.modalidad,
          tipoContrato: oferta.tipoContrato,
          salarioMinimo: oferta.salarioMinimo,
          salarioMaximo: oferta.salarioMaximo,
        });
      }
    });
  }

  hayCambiosSinGuardar(): boolean {
    return this.formulario.dirty && !this.guardado;
  }

  protected tieneError(campo: Campo): boolean {
    const control = this.formulario.controls[campo];
    return control.invalid && (control.touched || this.enviado());
  }

  protected mensaje(campo: Campo): string | null {
    return mensajeDeCampo(this.formulario.controls[campo], MENSAJES[campo]);
  }

  protected rangoSalarialNoValido(): boolean {
    return this.formulario.hasError('rangoSalarial') || this.formulario.hasError('servidor');
  }

  protected longitudDescripcion(): number {
    return this.formulario.controls.descripcion.value.length;
  }

  protected guardar(): void {
    this.enviado.set(true);
    this.error.set(null);
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      enfocarPrimerError(this.elemento.nativeElement, this.injector);
      return;
    }

    const valores = this.formulario.getRawValue();
    const oferta: OfertaRequest = {
      titulo: valores.titulo.trim(),
      descripcion: valores.descripcion.trim(),
      ubicacion: valores.ubicacion.trim(),
      modalidad: valores.modalidad!,
      tipoContrato: valores.tipoContrato!,
      salarioMinimo: valores.salarioMinimo!,
      salarioMaximo: valores.salarioMaximo!,
    };
    const id = this.id();
    const peticion: Observable<Oferta> = id
      ? this.api.actualizarOferta(id, oferta)
      : this.api.publicarOferta(oferta);

    this.enviando.set(true);
    peticion.pipe(finalize(() => this.enviando.set(false))).subscribe({
      next: (guardada) => {
        this.guardado = true;
        const aviso = id
          ? `Cambios guardados en "${guardada.titulo}".`
          : `"${guardada.titulo}" está publicada y ya aparece en el buscador.`;
        void this.router.navigateByUrl('/empresa', { state: { aviso } });
      },
      error: (error: unknown) => this.mostrarError(error),
    });
  }

  private mostrarError(error: unknown): void {
    const problema = problemaDe(error);
    if (aplicarErroresDelServidor(this.formulario, problema?.errores)) {
      enfocarPrimerError(this.elemento.nativeElement, this.injector);
    } else if (problema?.title === 'Rango salarial no válido') {
      this.formulario.setErrors({ servidor: problema.detail });
    } else {
      this.error.set(mensajeDeError(error));
    }
  }
}
