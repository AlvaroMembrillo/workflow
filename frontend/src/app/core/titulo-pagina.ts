import { inject, Injectable } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';

/** Título de la pestaña: "<título de la ruta> · Workflow". */
@Injectable({ providedIn: 'root' })
export class TituloPagina extends TitleStrategy {
  private readonly title = inject(Title);

  override updateTitle(estado: RouterStateSnapshot): void {
    const titulo = this.buildTitle(estado);
    this.title.setTitle(titulo ? `${titulo} · Workflow` : 'Workflow');
  }
}
