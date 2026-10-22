import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { Cuenta, Pagina } from '../models';
import { CuentaService } from './cuenta.service';

describe('CuentaService', () => {
  let service: CuentaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(CuentaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('listar hace GET a /cuentas con pagina=0 y tamano=100 y devuelve la página', () => {
    const pagina: Pagina<Cuenta> = {
      contenido: [
        {
          id: 1,
          numeroCuenta: '1000000001',
          titular: 'María Fernanda López',
          tipo: 'AHORRO',
          saldo: 15000,
          estado: 'ACTIVA',
          fechaCreacion: '2026-10-21T15:00:00Z'
        }
      ],
      pagina: 0,
      tamano: 100,
      totalElementos: 1,
      totalPaginas: 1
    };
    let resultado: Pagina<Cuenta> | undefined;

    service.listar().subscribe((p) => (resultado = p));

    const req = http.expectOne((r) => r.url === `${environment.apiUrl}/cuentas`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('pagina')).toBe('0');
    expect(req.request.params.get('tamano')).toBe('100');
    req.flush(pagina);
    expect(resultado).toEqual(pagina);
  });
});
