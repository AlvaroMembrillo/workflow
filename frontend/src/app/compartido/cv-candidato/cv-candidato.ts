import { HttpErrorResponse, HttpStatusCode, httpResource } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { LucideCircleAlert, LucideFileText } from '@lucide/angular';
import { finalize } from 'rxjs';

import { CvApi, TAMANO_MAXIMO_CV } from '../../core/api/cv-api';
import { Curriculum } from '../../core/api/modelos';
import { mensajeDeError, problemaDe } from '../../core/api/problema';
import { guardarFichero } from '../../core/descargas';
import { tamanoLegible } from '../../core/formato';
import { DialogoConfirmacion } from '../dialogo-confirmacion/dialogo-confirmacion';
import { Fecha } from '../fecha/fecha';

/**
 * El currículum del candidato: lo sube, lo sustituye, lo descarga o lo quita. Se usa en "Mi cuenta" y
 * al inscribirse en una oferta, porque es el mismo para todas sus candidaturas.
 */
@Component({
  selector: 'app-cv-candidato',
  imports: [Fecha, DialogoConfirmacion, LucideCircleAlert, LucideFileText],
  templateUrl: './cv-candidato.html',
  styleUrl: './cv-candidato.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CvCandidato {
  private readonly api = inject(CvApi);
  private readonly selector = viewChild.required<ElementRef<HTMLInputElement>>('selector');

  /** La API responde 404 mientras el candidato no haya subido ninguno. */
  private readonly guardado = httpResource<Curriculum>(() => '/api/candidatos/me/cv');

  protected readonly curriculum = computed(() =>
    this.guardado.hasValue() ? this.guardado.value() : null,
  );
  protected readonly cargando = this.guardado.isLoading;
  protected readonly noSePuedeCargar = computed(() => {
    const error = this.guardado.error();
    return !!error && (error as HttpErrorResponse).status !== HttpStatusCode.NotFound;
  });
  protected readonly tamano = computed(() => tamanoLegible(this.curriculum()?.tamano ?? 0));

  protected readonly ocupado = signal(false);
  protected readonly confirmandoQuitar = signal(false);
  /** Lo último que ha pasado, para quien usa lector de pantalla. */
  protected readonly novedad = signal('');
  protected readonly error = signal<string | null>(null);

  protected elegirFichero(): void {
    this.selector().nativeElement.click();
  }

  protected subir(evento: Event): void {
    const selector = evento.target as HTMLInputElement;
    const fichero = selector.files?.[0];
    // Se vacía para que elegir otra vez el mismo fichero vuelva a lanzar el evento
    selector.value = '';
    if (!fichero) {
      return;
    }
    this.novedad.set('');
    this.error.set(null);
    // Se comprueba aquí lo mismo que en la API para no subir 20 MB y descubrir después que no valen
    if (fichero.type !== 'application/pdf' && !fichero.name.toLowerCase().endsWith('.pdf')) {
      this.error.set('El currículum tiene que ser un PDF.');
      return;
    }
    if (fichero.size > TAMANO_MAXIMO_CV) {
      this.error.set(
        `Este fichero ocupa ${tamanoLegible(fichero.size)} y el máximo es 5 MB. Prueba a exportarlo con menos calidad de imagen.`,
      );
      return;
    }

    const sustituye = this.curriculum() !== null;
    this.ocupado.set(true);
    this.api
      .subir(fichero)
      .pipe(finalize(() => this.ocupado.set(false)))
      .subscribe({
        next: (curriculum) => {
          this.guardado.set(curriculum);
          this.novedad.set(sustituye ? 'Currículum sustituido.' : 'Currículum subido.');
        },
        error: (fallo: unknown) =>
          this.error.set(problemaDe(fallo)?.errores?.['fichero'] ?? mensajeDeError(fallo)),
      });
  }

  protected descargar(): void {
    const curriculum = this.curriculum();
    if (!curriculum) {
      return;
    }
    this.error.set(null);
    this.ocupado.set(true);
    this.api
      .descargarPropio()
      .pipe(finalize(() => this.ocupado.set(false)))
      .subscribe({
        next: (contenido) => guardarFichero(contenido, curriculum.nombreFichero),
        error: (fallo: unknown) => this.error.set(mensajeDeError(fallo)),
      });
  }

  protected quitar(): void {
    this.error.set(null);
    this.ocupado.set(true);
    this.api
      .borrar()
      .pipe(
        finalize(() => {
          this.ocupado.set(false);
          this.confirmandoQuitar.set(false);
        }),
      )
      .subscribe({
        next: () => {
          this.guardado.set(undefined);
          this.novedad.set('Currículum quitado. Las empresas ya no pueden descargarlo.');
        },
        error: (fallo: unknown) => this.error.set(mensajeDeError(fallo)),
      });
  }
}
