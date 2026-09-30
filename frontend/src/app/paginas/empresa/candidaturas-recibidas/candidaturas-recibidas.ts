import { HttpErrorResponse, HttpStatusCode, httpResource } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  inject,
  Injector,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideCircleAlert,
  LucideCircleCheck,
  LucideClock,
  LucideDownload,
  LucideMail,
} from '@lucide/angular';
import { finalize } from 'rxjs';

import { DialogoConfirmacion } from '../../../compartido/dialogo-confirmacion/dialogo-confirmacion';
import { EstadoCandidatura } from '../../../compartido/estado-candidatura/estado-candidatura';
import { EstadoVacio } from '../../../compartido/estado-vacio/estado-vacio';
import { leerPagina } from '../../../compartido/paginacion/leer-pagina';
import { Paginacion } from '../../../compartido/paginacion/paginacion';
import { ProgresoCandidatura } from '../../../compartido/progreso-candidatura/progreso-candidatura';
import { CvApi } from '../../../core/api/cv-api';
import {
  CandidaturaRecibida,
  EstadoCandidatura as Estado,
  OfertaConCandidaturas,
  Pagina,
  ResumenCandidaturas,
} from '../../../core/api/modelos';
import { PanelEmpresaApi } from '../../../core/api/panel-empresa-api';
import { mensajeDeError } from '../../../core/api/problema';
import { guardarFichero } from '../../../core/descargas';
import { enfocarTrasRender } from '../../../core/foco';
import { TEXTO_ESTADO_OFERTA } from '../../../core/textos';

export type Vista = 'sin-responder' | 'en-revision' | 'decididas' | 'todas';

interface Pestana {
  vista: Vista;
  texto: string;
  estados: Estado[];
  cuantas: (resumen: ResumenCandidaturas) => number;
}

export const PESTANAS: readonly Pestana[] = [
  {
    vista: 'sin-responder',
    texto: 'Sin responder',
    estados: ['PENDIENTE'],
    cuantas: (r) => r.pendientes,
  },
  {
    vista: 'en-revision',
    texto: 'En revisión',
    estados: ['EN_REVISION'],
    cuantas: (r) => r.enRevision,
  },
  {
    vista: 'decididas',
    texto: 'Decididas',
    estados: ['ACEPTADA', 'RECHAZADA', 'RETIRADA'],
    cuantas: (r) => r.aceptadas + r.rechazadas + r.retiradas,
  },
  { vista: 'todas', texto: 'Todas', estados: [], cuantas: (r) => r.total },
];

/** A partir de estos días sin respuesta, la espera se resalta para que la empresa responda. */
export const DIAS_DE_ESPERA_A_RESALTAR = 5;

const MS_POR_DIA = 24 * 60 * 60 * 1000;

export const leerVista = (valor: string | undefined): Vista =>
  PESTANAS.some((pestana) => pestana.vista === valor) ? (valor as Vista) : 'sin-responder';

interface Decision {
  candidatura: CandidaturaRecibida;
  estado: 'ACEPTADA' | 'RECHAZADA';
}

@Component({
  selector: 'app-candidaturas-recibidas',
  imports: [
    RouterLink,
    EstadoCandidatura,
    ProgresoCandidatura,
    EstadoVacio,
    Paginacion,
    DialogoConfirmacion,
    LucideCircleAlert,
    LucideCircleCheck,
    LucideClock,
    LucideDownload,
    LucideMail,
  ],
  templateUrl: './candidaturas-recibidas.html',
  styleUrl: './candidaturas-recibidas.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CandidaturasRecibidas {
  private readonly api = inject(PanelEmpresaApi);
  private readonly cvApi = inject(CvApi);
  private readonly injector = inject(Injector);
  private readonly mensajes = viewChild<ElementRef<HTMLElement>>('mensajes');

  /** Id de la oferta, de la ruta /empresa/ofertas/:id/candidaturas. */
  readonly id = input.required<string>();
  readonly vista = input<Vista, string | undefined>('sin-responder', { transform: leerVista });
  readonly pagina = input<number, string | undefined>(1, { transform: leerPagina });

  protected readonly pestanas = PESTANAS;
  protected readonly textoEstadoOferta = TEXTO_ESTADO_OFERTA;
  protected readonly pestanaActual = computed(() =>
    PESTANAS.find((pestana) => pestana.vista === this.vista())!,
  );

  protected readonly oferta = httpResource<OfertaConCandidaturas>(
    () => `/api/empresas/me/ofertas/${this.id()}`,
  );
  protected readonly esDeOtraEmpresa = computed(
    () =>
      (this.oferta.error() as HttpErrorResponse | undefined)?.status === HttpStatusCode.Forbidden,
  );

  protected readonly candidaturas = httpResource<Pagina<CandidaturaRecibida>>(() => ({
    url: `/api/ofertas/${this.id()}/candidaturas`,
    params: {
      estado: this.pestanaActual().estados,
      page: this.pagina() - 1,
      size: 20,
      // Las que esperan respuesta, de la más antigua a la más reciente
      sort: this.vista() === 'sin-responder' ? 'fechaCreacion,asc' : 'fechaCreacion,desc',
    },
  }));

  protected readonly decision = signal<Decision | null>(null);
  protected readonly cambiando = signal<string | null>(null);
  /** Id de la candidatura cuyo currículum se está descargando. */
  protected readonly descargando = signal<string | null>(null);
  protected readonly aviso = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly tituloDecision = computed(() => {
    const decision = this.decision();
    if (!decision) return '';
    const accion = decision.estado === 'ACEPTADA' ? 'Aceptar' : 'Rechazar';
    return `¿${accion} la candidatura de ${decision.candidatura.candidato.nombre}?`;
  });

  protected readonly textoDecision = computed(() => {
    const decision = this.decision();
    if (!decision) return '';
    const textoCandidato = decision.estado === 'ACEPTADA' ? 'Seleccionada' : 'No seleccionada';
    return `Es una decisión final: no se puede cambiar. ${decision.candidatura.candidato.nombre} verá "${textoCandidato}" en sus candidaturas.`;
  });

  protected cuantas(pestana: Pestana): number | null {
    return this.oferta.hasValue() ? pestana.cuantas(this.oferta.value().candidaturas) : null;
  }

  /** Días que lleva una candidatura pendiente sin respuesta, o null si no hay que destacarlo. */
  protected diasSinRespuesta(candidatura: CandidaturaRecibida): number | null {
    if (candidatura.estado !== 'PENDIENTE') return null;
    const dias = Math.floor(
      (Date.now() - new Date(candidatura.fechaCreacion).getTime()) / MS_POR_DIA,
    );
    return dias >= DIAS_DE_ESPERA_A_RESALTAR ? dias : null;
  }

  protected pasarARevision(candidatura: CandidaturaRecibida): void {
    this.cambiarEstado(candidatura, 'EN_REVISION');
  }

  protected pedirDecision(
    candidatura: CandidaturaRecibida,
    estado: 'ACEPTADA' | 'RECHAZADA',
  ): void {
    this.aviso.set(null);
    this.error.set(null);
    this.decision.set({ candidatura, estado });
  }

  protected confirmarDecision(): void {
    const decision = this.decision();
    if (decision) {
      this.cambiarEstado(decision.candidatura, decision.estado);
    }
  }

  protected descargarCv(candidatura: CandidaturaRecibida): void {
    const cv = candidatura.cv;
    if (!cv) {
      return;
    }
    this.descargando.set(candidatura.id);
    this.error.set(null);
    this.cvApi
      .descargarDeCandidatura(candidatura.id)
      .pipe(finalize(() => this.descargando.set(null)))
      .subscribe({
        next: (contenido) => guardarFichero(contenido, cv.nombreFichero),
        error: (error: unknown) => {
          // 404: el candidato ha quitado su currículum o ha retirado la candidatura después de cargar la lista
          const yaNoEsta =
            error instanceof HttpErrorResponse && error.status === HttpStatusCode.NotFound;
          this.error.set(
            yaNoEsta
              ? `${candidatura.candidato.nombre} ha quitado su currículum o ha retirado la candidatura.`
              : mensajeDeError(error),
          );
          if (yaNoEsta) {
            this.recargar();
          }
          enfocarTrasRender(this.mensajes, this.injector);
        },
      });
  }

  private cambiarEstado(candidatura: CandidaturaRecibida, estado: Estado): void {
    this.cambiando.set(candidatura.id);
    this.aviso.set(null);
    this.error.set(null);
    this.api
      .cambiarEstadoCandidatura(candidatura.id, estado)
      .pipe(
        finalize(() => {
          this.cambiando.set(null);
          this.decision.set(null);
        }),
      )
      .subscribe({
        next: () => {
          const nombre = candidatura.candidato.nombre;
          this.aviso.set(
            estado === 'EN_REVISION'
              ? `La candidatura de ${nombre} pasa a revisión.`
              : estado === 'ACEPTADA'
                ? `Has aceptado la candidatura de ${nombre}.`
                : `Has rechazado la candidatura de ${nombre}.`,
          );
          this.recargar();
          enfocarTrasRender(this.mensajes, this.injector);
        },
        error: (error: unknown) => {
          // 409: el candidato la ha retirado o ya estaba decidida. Se recarga para ver su estado real
          this.error.set(mensajeDeError(error));
          this.recargar();
          enfocarTrasRender(this.mensajes, this.injector);
        },
      });
  }

  private recargar(): void {
    this.candidaturas.reload();
    this.oferta.reload();
  }
}
