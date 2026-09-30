import { ChangeDetectionStrategy, Component, ElementRef, inject, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { distinctUntilChanged, filter, map, skip } from 'rxjs';

import { AvisoVerificacion } from './layout/aviso-verificacion/aviso-verificacion';
import { Cabecera } from './layout/cabecera/cabecera';
import { Pie } from './layout/pie/pie';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Cabecera, AvisoVerificacion, Pie],
  templateUrl: './app.html',
  styleUrl: './app.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  private readonly contenido = viewChild.required<ElementRef<HTMLElement>>('contenido');

  constructor() {
    // Al cambiar de página, el foco va al contenido nuevo para que el lector de pantalla empiece por él
    // en lugar de quedarse en el enlace que se pulsó. Solo cuenta el cambio de ruta: cambiar filtros o
    // la página de resultados (?pagina=) no mueve el foco del control que se está usando.
    // La primera carga tampoco lo mueve.
    inject(Router)
      .events.pipe(
        filter((evento) => evento instanceof NavigationEnd),
        map((evento) => evento.urlAfterRedirects.split(/[?#]/)[0]),
        distinctUntilChanged(),
        skip(1),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.contenido().nativeElement.focus({ preventScroll: true }));
  }
}
