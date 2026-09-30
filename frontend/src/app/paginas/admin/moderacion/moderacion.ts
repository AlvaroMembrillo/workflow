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
import { LucideCircleAlert, LucideCircleCheck, LucideFlag } from '@lucide/angular';
import { finalize, Observable } from 'rxjs';

import { DialogoConfirmacion } from '../../../compartido/dialogo-confirmacion/dialogo-confirmacion';
import { EstadoVacio } from '../../../compartido/estado-vacio/estado-vacio';
import { Fecha } from '../../../compartido/fecha/fecha';
import { leerPagina } from '../../../compartido/paginacion/leer-pagina';
import { Paginacion } from '../../../compartido/paginacion/paginacion';
import { ModeracionApi } from '../../../core/api/moderacion-api';
import { Denuncia, Pagina } from '../../../core/api/modelos';
import { mensajeDeError } from '../../../core/api/problema';
import { enfocarTrasRender } from '../../../core/foco';
import { TEXTO_MOTIVO_DENUNCIA } from '../../../core/textos';

type Accion = 'retirar' | 'suspender';

interface Confirmacion {
  accion: Accion;
  denuncia: Denuncia;
}

/** Panel de moderación: las denuncias pendientes, de la más antigua a la más reciente. */
@Component({
  selector: 'app-moderacion',
  imports: [
    Fecha,
    EstadoVacio,
    Paginacion,
    DialogoConfirmacion,
    LucideCircleAlert,
    LucideCircleCheck,
    LucideFlag,
  ],
  templateUrl: './moderacion.html',
  styleUrl: './moderacion.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Moderacion {
  private readonly api = inject(ModeracionApi);
  private readonly injector = inject(Injector);
  private readonly mensajes = viewChild<ElementRef<HTMLElement>>('mensajes');

  readonly pagina = input<number, string | undefined>(1, { transform: leerPagina });

  protected readonly denuncias = httpResource<Pagina<Denuncia>>(() => ({
    url: '/api/admin/denuncias',
    params: { estado: 'PENDIENTE', page: this.pagina() - 1, size: 20 },
  }));

  protected readonly textoMotivo = TEXTO_MOTIVO_DENUNCIA;
  protected readonly confirmacion = signal<Confirmacion | null>(null);
  /** Id de la denuncia sobre la que se está actuando. */
  protected readonly ocupada = signal<string | null>(null);
  protected readonly aviso = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly tituloConfirmacion = computed(() => {
    const confirmacion = this.confirmacion();
    if (!confirmacion) {
      return '';
    }
    return confirmacion.accion === 'retirar'
      ? `¿Retirar la oferta "${confirmacion.denuncia.oferta.titulo}"?`
      : `¿Suspender la cuenta de ${confirmacion.denuncia.oferta.empresaNombre}?`;
  });
  protected readonly textoConfirmacion = computed(() =>
    this.confirmacion()?.accion === 'suspender'
      ? 'Se retirarán todas sus ofertas, se cerrarán sus sesiones y no podrá volver a entrar. Le avisaremos por correo.'
      : 'Dejará de verse en el buscador y la empresa no podrá reabrirla. Le avisaremos por correo.',
  );

  protected desestimar(denuncia: Denuncia): void {
    this.ejecutar(
      denuncia,
      this.api.resolver(denuncia.id, 'DESESTIMAR'),
      `Denuncia de "${denuncia.oferta.titulo}" desestimada.`,
    );
  }

  protected confirmar(): void {
    const confirmacion = this.confirmacion();
    if (!confirmacion) {
      return;
    }
    const { accion, denuncia } = confirmacion;
    if (accion === 'retirar') {
      this.ejecutar(
        denuncia,
        this.api.resolver(denuncia.id, 'RETIRAR_OFERTA'),
        `Oferta "${denuncia.oferta.titulo}" retirada.`,
      );
    } else {
      this.ejecutar(
        denuncia,
        this.api.suspenderEmpresa(denuncia.oferta.empresaId),
        `Cuenta de ${denuncia.oferta.empresaNombre} suspendida y sus ofertas retiradas.`,
      );
    }
  }

  private ejecutar(denuncia: Denuncia, peticion: Observable<unknown>, hecho: string): void {
    this.ocupada.set(denuncia.id);
    this.aviso.set(null);
    this.error.set(null);
    peticion
      .pipe(
        finalize(() => {
          this.ocupada.set(null);
          this.confirmacion.set(null);
        }),
      )
      .subscribe({
        next: () => {
          this.aviso.set(hecho);
          this.denuncias.reload();
          // La denuncia desaparece de la lista: el foco va al mensaje que explica qué ha pasado
          enfocarTrasRender(this.mensajes, this.injector);
        },
        error: (fallo: unknown) => {
          // 409: otra persona del equipo ya la ha resuelto. Se recarga para ver la lista real
          this.error.set(mensajeDeError(fallo));
          this.denuncias.reload();
          enfocarTrasRender(this.mensajes, this.injector);
        },
      });
  }
}
