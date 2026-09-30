import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Curriculum } from './modelos';

/** Tamaño máximo del currículum, el mismo que acepta la API. */
export const TAMANO_MAXIMO_CV = 5 * 1024 * 1024;

/** Subida y descarga de currículos. La consulta del propio se hace con httpResource. */
@Injectable({ providedIn: 'root' })
export class CvApi {
  private readonly http = inject(HttpClient);

  /** Sube el currículum del candidato; si ya tenía uno, lo sustituye. */
  subir(fichero: File): Observable<Curriculum> {
    const formulario = new FormData();
    formulario.append('fichero', fichero);
    return this.http.post<Curriculum>('/api/candidatos/me/cv', formulario);
  }

  borrar(): Observable<void> {
    return this.http.delete<void>('/api/candidatos/me/cv');
  }

  descargarPropio(): Observable<Blob> {
    return this.http.get('/api/candidatos/me/cv/fichero', { responseType: 'blob' });
  }

  /** El currículum del candidato de una candidatura recibida, para la empresa de la oferta. */
  descargarDeCandidatura(candidaturaId: string): Observable<Blob> {
    return this.http.get(`/api/candidaturas/${candidaturaId}/cv`, { responseType: 'blob' });
  }
}
