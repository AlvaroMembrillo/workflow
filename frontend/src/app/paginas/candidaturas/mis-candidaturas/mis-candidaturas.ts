import { httpResource } from '@angular/common/http';
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
import { LucideCircleAlert, LucideInfo } from '@lucide/angular';
import { finalize } from 'rxjs';

import { DialogoConfirmacion } from '../../../compartido/dialogo-confirmacion/dialogo-confirmacion';
import { EstadoCandidatura } from '../../../compartido/estado-candidatura/estado-candidatura';
import { EstadoVacio } from '../../../compartido/estado-vacio/estado-vacio';
import { Fecha } from '../../../compartido/fecha/fecha';
import { leerPagina } from '../../../compartido/paginacion/leer-pagina';
import { Paginacion } from '../../../compartido/paginacion/paginacion';
import { ProgresoCandidatura } from '../../../compartido/progreso-candidatura/progreso-candidatura';
import { CandidaturasApi } from '../../../core/api/candidaturas-api';
import { MiCandidatura, Pagina } from '../../../core/api/modelos';
import { mensajeDeError } from '../../../core/api/problema';
import { enfocarTrasRender } from '../../../core/foco';

const TAMANO_PAGINA = 10;

@Component({
  selector: 'app-mis-candidaturas',
  imports: [
    RouterLink,
    EstadoCandidatura,
    ProgresoCandidatura,
    Fecha,
    EstadoVacio,
    Paginacion,
    DialogoConfirmacion,
    LucideCircleAlert,
    LucideInfo,
  ],
  templateUrl: './mis-candidaturas.html',
  styleUrl: './mis-candidaturas.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisCandidaturas {
  private readonly candidaturasApi = inject(CandidaturasApi);
  private readonly injector = inject(Injector);
  private readonly mensajes = viewChild<ElementRef<HTMLElement>>('mensajes');

  readonly pagina = input<number, string | undefined>(1, { transform: leerPagina });

  protected readonly candidaturas = httpResource<Pagina<MiCandidatura>>(() => ({
    url: '/api/candidaturas/me',
    params: { page: this.pagina() - 1, size: TAMANO_PAGINA },
  }));

  /** Candidatura que se va a retirar; mientras no sea null, el diálogo de confirmación está abierto. */
  protected readonly aRetirar = signal<MiCandidatura | null>(null);
  protected readonly retirando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly aviso = signal<string | null>(null);

  protected readonly textoConfirmacion = computed(() => {
    const candidatura = this.aRetirar();
    return candidatura
      ? `${candidatura.oferta.empresa.nombre} verá que la has retirado y no podrás volver a inscribirte en "${candidatura.oferta.titulo}".`
      : '';
  });

  protected sePuedeRetirar(candidatura: MiCandidatura): boolean {
    return candidatura.estado === 'PENDIENTE' || candidatura.estado === 'EN_REVISION';
  }

  protected pedirRetirada(candidatura: MiCandidatura): void {
    this.error.set(null);
    this.aviso.set(null);
    this.aRetirar.set(candidatura);
  }

  protected retirar(): void {
    const candidatura = this.aRetirar();
    if (!candidatura) {
      return;
    }
    this.retirando.set(true);
    this.candidaturasApi
      .retirar(candidatura.id)
      .pipe(
        finalize(() => {
          this.retirando.set(false);
          this.aRetirar.set(null);
        }),
      )
      .subscribe({
        next: () => {
          this.aviso.set(`Has retirado tu candidatura a "${candidatura.oferta.titulo}".`);
          this.candidaturas.reload();
          enfocarTrasRender(this.mensajes, this.injector);
        },
        error: (error: unknown) => {
          this.error.set(mensajeDeError(error));
          this.candidaturas.reload();
          enfocarTrasRender(this.mensajes, this.injector);
        },
      });
  }
}
