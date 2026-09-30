import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideCircleAlert, LucideCircleCheck, LucidePencil, LucideUsers } from '@lucide/angular';
import { finalize } from 'rxjs';

import { EstadoVacio } from '../../../compartido/estado-vacio/estado-vacio';
import { Fecha } from '../../../compartido/fecha/fecha';
import { leerPagina } from '../../../compartido/paginacion/leer-pagina';
import { Paginacion } from '../../../compartido/paginacion/paginacion';
import { Salario } from '../../../compartido/salario/salario';
import { OfertaConCandidaturas, Pagina } from '../../../core/api/modelos';
import { PanelEmpresaApi } from '../../../core/api/panel-empresa-api';
import { mensajeDeError } from '../../../core/api/problema';
import { TEXTO_ESTADO_OFERTA, TEXTO_MODALIDAD } from '../../../core/textos';

const TAMANO_PAGINA = 10;

@Component({
  selector: 'app-mis-ofertas',
  imports: [
    RouterLink,
    Salario,
    Fecha,
    EstadoVacio,
    Paginacion,
    LucideCircleAlert,
    LucideCircleCheck,
    LucidePencil,
    LucideUsers,
  ],
  templateUrl: './mis-ofertas.html',
  styleUrl: './mis-ofertas.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisOfertas {
  private readonly api = inject(PanelEmpresaApi);

  readonly pagina = input<number, string | undefined>(1, { transform: leerPagina });

  protected readonly ofertas = httpResource<Pagina<OfertaConCandidaturas>>(() => ({
    url: '/api/empresas/me/ofertas',
    params: { page: this.pagina() - 1, size: TAMANO_PAGINA },
  }));

  /** Mensaje tras publicar o guardar una oferta (llega en el estado de la navegación). */
  protected readonly aviso = signal<string | null>(leerAvisoDeNavegacion());
  protected readonly error = signal<string | null>(null);
  /** Oferta que se está abriendo o cerrando, para desactivar su botón mientras tanto. */
  protected readonly cambiando = signal<string | null>(null);

  protected readonly textoModalidad = TEXTO_MODALIDAD;
  protected readonly textoEstado = TEXTO_ESTADO_OFERTA;

  protected cambiarEstado({ oferta }: OfertaConCandidaturas): void {
    const nuevo = oferta.estado === 'ABIERTA' ? 'CERRADA' : 'ABIERTA';
    this.cambiando.set(oferta.id);
    this.error.set(null);
    this.api
      .cambiarEstadoOferta(oferta.id, nuevo)
      .pipe(finalize(() => this.cambiando.set(null)))
      .subscribe({
        next: () => {
          this.aviso.set(
            nuevo === 'CERRADA'
              ? `"${oferta.titulo}" está cerrada: ya no admite candidaturas ni aparece en el buscador.`
              : `"${oferta.titulo}" vuelve a estar abierta.`,
          );
          this.ofertas.reload();
        },
        error: (error: unknown) => this.error.set(mensajeDeError(error)),
      });
  }
}

/**
 * Lee el aviso que deja la pantalla anterior en el estado del historial y lo borra, para que no vuelva
 * a salir al recargar la página. Se conserva el resto del estado, que usa el router.
 */
function leerAvisoDeNavegacion(): string | null {
  const estado: unknown = history.state;
  if (typeof estado !== 'object' || estado === null || !('aviso' in estado)) {
    return null;
  }
  const { aviso, ...resto } = estado as { aviso: unknown };
  history.replaceState(resto, '');
  return String(aviso);
}
