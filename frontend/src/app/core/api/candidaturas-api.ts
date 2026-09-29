import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { MiCandidatura } from './modelos';

/** Operaciones sobre candidaturas. Las consultas se hacen con httpResource en cada pantalla. */
@Injectable({ providedIn: 'root' })
export class CandidaturasApi {
  private readonly http = inject(HttpClient);

  inscribirse(ofertaId: string, cartaPresentacion: string | null): Observable<MiCandidatura> {
    return this.http.post<MiCandidatura>(`/api/ofertas/${ofertaId}/candidaturas`, {
      cartaPresentacion,
    });
  }

  /** El candidato retira su candidatura mientras la empresa no haya decidido. */
  retirar(candidaturaId: string): Observable<MiCandidatura> {
    return this.http.post<MiCandidatura>(`/api/candidaturas/${candidaturaId}/retirada`, {});
  }
}
