import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Transferencia, TransferenciaRequest } from '../models';

@Injectable({ providedIn: 'root' })
export class TransferenciaService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/transferencias`;

  ejecutar(request: TransferenciaRequest): Observable<Transferencia> {
    return this.http.post<Transferencia>(this.url, request);
  }
}
