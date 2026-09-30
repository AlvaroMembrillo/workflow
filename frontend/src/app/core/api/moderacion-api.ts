import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Denuncia, MotivoDenuncia } from './modelos';

/** Denuncias de ofertas y acciones del panel de moderación. */
@Injectable({ providedIn: 'root' })
export class ModeracionApi {
  private readonly http = inject(HttpClient);

  denunciar(ofertaId: string, motivo: MotivoDenuncia, detalle: string | null): Observable<void> {
    return this.http.post<void>(`/api/ofertas/${ofertaId}/denuncias`, { motivo, detalle });
  }

  resolver(denunciaId: string, accion: 'RETIRAR_OFERTA' | 'DESESTIMAR'): Observable<Denuncia> {
    return this.http.post<Denuncia>(`/api/admin/denuncias/${denunciaId}/resolucion`, { accion });
  }

  suspenderEmpresa(empresaId: string): Observable<void> {
    return this.http.post<void>(`/api/admin/empresas/${empresaId}/suspension`, null);
  }
}
