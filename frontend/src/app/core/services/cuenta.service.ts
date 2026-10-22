import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Cuenta, Pagina } from '../models';

@Injectable({ providedIn: 'root' })
export class CuentaService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/cuentas`;

  listar(pagina = 0, tamano = 100): Observable<Pagina<Cuenta>> {
    const params = new HttpParams().set('pagina', pagina).set('tamano', tamano);
    return this.http.get<Pagina<Cuenta>>(this.url, { params });
  }
}
