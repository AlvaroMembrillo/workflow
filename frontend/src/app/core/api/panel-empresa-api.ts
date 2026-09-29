import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  CandidaturaRecibida,
  Empresa,
  EmpresaRequest,
  EstadoCandidatura,
  EstadoOferta,
  Oferta,
  OfertaRequest,
} from './modelos';

/** Operaciones del panel de empresa. Las consultas se hacen con httpResource en cada pantalla. */
@Injectable({ providedIn: 'root' })
export class PanelEmpresaApi {
  private readonly http = inject(HttpClient);

  publicarOferta(oferta: OfertaRequest): Observable<Oferta> {
    return this.http.post<Oferta>('/api/ofertas', oferta);
  }

  actualizarOferta(id: string, oferta: OfertaRequest): Observable<Oferta> {
    return this.http.put<Oferta>(`/api/ofertas/${id}`, oferta);
  }

  cambiarEstadoOferta(id: string, estado: EstadoOferta): Observable<Oferta> {
    return this.http.patch<Oferta>(`/api/ofertas/${id}/estado`, { estado });
  }

  cambiarEstadoCandidatura(id: string, estado: EstadoCandidatura): Observable<CandidaturaRecibida> {
    return this.http.patch<CandidaturaRecibida>(`/api/candidaturas/${id}/estado`, { estado });
  }

  actualizarPerfil(perfil: EmpresaRequest): Observable<Empresa> {
    return this.http.put<Empresa>('/api/empresas/me', perfil);
  }
}
