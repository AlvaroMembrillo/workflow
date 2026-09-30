import { HttpErrorResponse, HttpStatusCode, httpResource } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  linkedSignal,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { LucideCircleAlert, LucideCircleCheck, LucideFileText, LucideLock } from '@lucide/angular';
import { finalize } from 'rxjs';

import { CvCandidato } from '../../../compartido/cv-candidato/cv-candidato';
import { EstadoVacio } from '../../../compartido/estado-vacio/estado-vacio';
import { Fecha } from '../../../compartido/fecha/fecha';
import { Salario } from '../../../compartido/salario/salario';
import { CandidaturasApi } from '../../../core/api/candidaturas-api';
import { MiCandidatura, Oferta } from '../../../core/api/modelos';
import { mensajeDeError } from '../../../core/api/problema';
import { Sesion } from '../../../core/auth/sesion';
import {
  TEXTO_ESTADO_PARA_CANDIDATO,
  TEXTO_MODALIDAD,
  TEXTO_TIPO_CONTRATO,
} from '../../../core/textos';

export const LONGITUD_MAXIMA_CARTA = 5000;

@Component({
  selector: 'app-detalle-oferta',
  imports: [
    FormsModule,
    RouterLink,
    Salario,
    Fecha,
    EstadoVacio,
    CvCandidato,
    LucideCircleAlert,
    LucideCircleCheck,
    LucideFileText,
    LucideLock,
  ],
  templateUrl: './detalle.html',
  styleUrl: './detalle.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Detalle {
  private readonly candidaturasApi = inject(CandidaturasApi);
  private readonly titulo = inject(Title);
  protected readonly sesion = inject(Sesion);

  /** Id de la oferta, de la ruta /ofertas/:id. */
  readonly id = input.required<string>();

  protected readonly oferta = httpResource<Oferta>(() => `/api/ofertas/${this.id()}`);

  /** Si el candidato ya se inscribió (404 si no). Solo se consulta con sesión de candidato. */
  private readonly candidaturaGuardada = httpResource<MiCandidatura>(() =>
    this.sesion.rol() === 'CANDIDATO' ? `/api/ofertas/${this.id()}/mi-candidatura` : undefined,
  );
  /** La candidatura recién enviada desde esta pantalla. Se olvida al cambiar de oferta. */
  private readonly candidaturaNueva = linkedSignal<string, MiCandidatura | null>({
    source: this.id,
    computation: () => null,
  });

  protected readonly miCandidatura = computed(
    () =>
      this.candidaturaNueva() ??
      (this.candidaturaGuardada.hasValue() ? this.candidaturaGuardada.value() : null),
  );
  protected readonly comprobandoCandidatura = computed(
    () => this.sesion.rol() === 'CANDIDATO' && this.candidaturaGuardada.isLoading(),
  );

  protected readonly noEncontrada = computed(
    () =>
      (this.oferta.error() as HttpErrorResponse | undefined)?.status === HttpStatusCode.NotFound,
  );
  protected readonly rutaActual = computed(() => `/ofertas/${this.id()}`);

  protected readonly mostrarCarta = signal(false);
  protected readonly carta = signal('');
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly textoModalidad = TEXTO_MODALIDAD;
  protected readonly textoContrato = TEXTO_TIPO_CONTRATO;
  protected readonly textoEstado = TEXTO_ESTADO_PARA_CANDIDATO;
  protected readonly longitudMaximaCarta = LONGITUD_MAXIMA_CARTA;

  constructor() {
    effect(() => {
      if (this.oferta.hasValue()) {
        this.titulo.setTitle(`${this.oferta.value().titulo} · Workflow`);
      }
    });
  }

  protected inscribirse(evento: Event): void {
    evento.preventDefault();
    this.enviando.set(true);
    this.error.set(null);
    this.candidaturasApi
      .inscribirse(this.id(), this.carta().trim() || null)
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: (candidatura) => this.candidaturaNueva.set(candidatura),
        error: (error: unknown) => {
          this.error.set(mensajeDeError(error));
          // 409: ya inscrito o la oferta acaba de cerrarse. Se recarga para mostrar el estado real
          if (error instanceof HttpErrorResponse && error.status === HttpStatusCode.Conflict) {
            this.oferta.reload();
            this.candidaturaGuardada.reload();
          }
        },
      });
  }
}
