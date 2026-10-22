import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { Transferencia, TransferenciaRequest } from '../models';
import { TransferenciaService } from './transferencia.service';

describe('TransferenciaService', () => {
  let service: TransferenciaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(TransferenciaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('ejecutar hace POST con el cuerpo exacto y devuelve la transferencia', () => {
    const request: TransferenciaRequest = { cuentaOrigen: '1000000001', cuentaDestino: '1000000002', monto: 250.5 };
    const respuesta: Transferencia = {
      id: 7,
      ...request,
      estado: 'COMPLETADA',
      motivo: null,
      fecha: '2026-10-21T15:00:00Z'
    };
    let resultado: Transferencia | undefined;

    service.ejecutar(request).subscribe((t) => (resultado = t));

    const req = http.expectOne(`${environment.apiUrl}/transferencias`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(respuesta);
    expect(resultado).toEqual(respuesta);
  });
});
