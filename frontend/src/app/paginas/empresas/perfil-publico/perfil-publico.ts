import { HttpErrorResponse, HttpStatusCode, httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { LucideCircleAlert, LucideExternalLink, LucideMapPin } from '@lucide/angular';

import { EstadoVacio } from '../../../compartido/estado-vacio/estado-vacio';
import { TarjetaOferta } from '../../../compartido/tarjeta-oferta/tarjeta-oferta';
import { Empresa, Oferta, Pagina } from '../../../core/api/modelos';

/** Solo se enlazan webs http(s): el perfil lo escribe la empresa y no debe poder colar otros esquemas. */
export function esWebSegura(url: string | null): url is string {
  return !!url && /^https?:\/\//i.test(url);
}

@Component({
  selector: 'app-perfil-publico',
  imports: [
    RouterLink,
    TarjetaOferta,
    EstadoVacio,
    LucideMapPin,
    LucideExternalLink,
    LucideCircleAlert,
  ],
  templateUrl: './perfil-publico.html',
  styleUrl: './perfil-publico.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PerfilPublico {
  private readonly titulo = inject(Title);

  /** Id de la empresa, de la ruta /empresas/:id. */
  readonly id = input.required<string>();

  protected readonly empresa = httpResource<Empresa>(() => `/api/empresas/${this.id()}`);
  protected readonly ofertas = httpResource<Pagina<Oferta>>(() => ({
    url: '/api/ofertas',
    params: { empresaId: this.id(), size: 50 },
  }));

  protected readonly noEncontrada = computed(
    () =>
      (this.empresa.error() as HttpErrorResponse | undefined)?.status === HttpStatusCode.NotFound,
  );
  protected readonly web = computed(() => {
    const url = this.empresa.hasValue() ? this.empresa.value().sitioWeb : null;
    return esWebSegura(url) ? url : null;
  });
  /** La web sin "https://" ni barra final, para mostrarla. */
  protected readonly webVisible = computed(() =>
    this.web()
      ?.replace(/^https?:\/\//i, '')
      .replace(/\/$/, ''),
  );

  constructor() {
    effect(() => {
      if (this.empresa.hasValue()) {
        this.titulo.setTitle(`${this.empresa.value().nombre} · Workflow`);
      }
    });
  }
}
