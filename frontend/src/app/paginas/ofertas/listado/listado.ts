import { httpResource } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  input,
  linkedSignal,
  untracked,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  LucideCircleAlert,
  LucideMapPin,
  LucideSearch,
  LucideSlidersHorizontal,
  LucideX,
} from '@lucide/angular';

import { EstadoVacio } from '../../../compartido/estado-vacio/estado-vacio';
import { leerPagina } from '../../../compartido/paginacion/leer-pagina';
import { Paginacion } from '../../../compartido/paginacion/paginacion';
import { TarjetaOferta } from '../../../compartido/tarjeta-oferta/tarjeta-oferta';
import { Modalidad, Oferta, Pagina, TipoContrato } from '../../../core/api/modelos';
import { TEXTO_MODALIDAD, TEXTO_TIPO_CONTRATO } from '../../../core/textos';
import {
  FiltroOfertas,
  hayFiltros,
  leerModalidad,
  leerTexto,
  leerTipoContrato,
  parametrosApi,
} from './filtros';
import { CambioFiltros, PanelFiltros } from './panel-filtros';

interface Chip {
  clave: keyof FiltroOfertas;
  texto: string;
}

const NUMERO = new Intl.NumberFormat('es-ES');

/**
 * Portada: buscador de ofertas. Los filtros vienen de la URL (?texto=…&modalidad=…&pagina=…) como inputs
 * del componente, así la búsqueda se puede compartir y el botón "atrás" funciona.
 */
@Component({
  selector: 'app-listado-ofertas',
  imports: [
    FormsModule,
    TarjetaOferta,
    Paginacion,
    EstadoVacio,
    PanelFiltros,
    LucideSearch,
    LucideMapPin,
    LucideSlidersHorizontal,
    LucideX,
    LucideCircleAlert,
  ],
  templateUrl: './listado.html',
  styleUrl: './listado.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Listado {
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  readonly texto = input<string, string | undefined>('', { transform: leerTexto });
  readonly ubicacion = input<string, string | undefined>('', { transform: leerTexto });
  readonly modalidad = input<Modalidad | null, string | undefined>(null, {
    transform: leerModalidad,
  });
  readonly tipoContrato = input<TipoContrato | null, string | undefined>(null, {
    transform: leerTipoContrato,
  });
  readonly pagina = input<number, string | undefined>(1, { transform: leerPagina });

  protected readonly filtro = computed<FiltroOfertas>(() => ({
    texto: this.texto(),
    ubicacion: this.ubicacion(),
    modalidad: this.modalidad(),
    tipoContrato: this.tipoContrato(),
  }));

  // Lo que el usuario escribe en el buscador. Se reinicia cuando cambia la URL (por ejemplo, al quitar un filtro)
  protected readonly textoBuscado = linkedSignal(() => this.texto());
  protected readonly ubicacionBuscada = linkedSignal(() => this.ubicacion());

  protected readonly ofertas = httpResource<Pagina<Oferta>>(() => ({
    url: '/api/ofertas',
    params: parametrosApi(this.filtro(), this.pagina()),
  }));

  protected readonly total = computed(() =>
    this.ofertas.hasValue() ? this.ofertas.value().page.totalElements : null,
  );

  protected readonly textoTotal = computed(() => {
    const total = this.total();
    if (total === null) {
      return 'Buscando ofertas…';
    }
    return total === 1 ? '1 oferta' : `${NUMERO.format(total)} ofertas`;
  });

  protected readonly hayFiltros = computed(() => hayFiltros(this.filtro()));

  /** Filtros que están en la hoja de móvil, para indicarlo en su botón. */
  protected readonly filtrosEnHoja = computed(
    () => [this.modalidad(), this.tipoContrato()].filter(Boolean).length,
  );

  protected readonly chips = computed<Chip[]>(() => {
    const filtro = this.filtro();
    const chips: Chip[] = [];
    if (filtro.texto) chips.push({ clave: 'texto', texto: `"${filtro.texto}"` });
    if (filtro.ubicacion) chips.push({ clave: 'ubicacion', texto: filtro.ubicacion });
    if (filtro.modalidad)
      chips.push({ clave: 'modalidad', texto: TEXTO_MODALIDAD[filtro.modalidad] });
    if (filtro.tipoContrato) {
      chips.push({ clave: 'tipoContrato', texto: TEXTO_TIPO_CONTRATO[filtro.tipoContrato] });
    }
    return chips;
  });

  private readonly hoja = viewChild.required<ElementRef<HTMLDialogElement>>('hojaFiltros');
  private readonly totalResultados = viewChild.required<ElementRef<HTMLElement>>('totalResultados');

  constructor() {
    // Al cambiar de página de resultados, el foco va al número de resultados: el enlace pulsado
    // ("Siguiente") puede desaparecer y el foco se perdería
    let primeraVez = true;
    effect(() => {
      this.pagina();
      if (primeraVez) {
        primeraVez = false;
        return;
      }
      untracked(() => this.totalResultados().nativeElement.focus());
    });
  }

  protected buscar(evento: Event): void {
    evento.preventDefault();
    this.navegar({
      texto: this.textoBuscado().trim() || null,
      ubicacion: this.ubicacionBuscada().trim() || null,
    });
  }

  protected cambiarFiltros(cambio: CambioFiltros): void {
    this.navegar({ ...cambio });
  }

  protected quitar(clave: keyof FiltroOfertas): void {
    this.navegar({ [clave]: null });
  }

  protected borrarFiltros(): void {
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: {} });
  }

  protected abrirFiltros(): void {
    this.hoja().nativeElement.showModal();
  }

  protected cerrarFiltros(): void {
    this.hoja().nativeElement.close();
  }

  /** Un clic fuera de la hoja (en el fondo oscurecido) la cierra. */
  protected alPulsarHoja(evento: MouseEvent): void {
    if (evento.target === this.hoja().nativeElement) {
      this.cerrarFiltros();
    }
  }

  /** Aplica cambios en la URL. Cualquier cambio de filtros vuelve a la primera página. */
  private navegar(cambios: Record<string, string | null>): void {
    void this.router.navigate([], {
      relativeTo: this.ruta,
      queryParams: { ...cambios, pagina: null },
      queryParamsHandling: 'merge',
    });
  }
}
