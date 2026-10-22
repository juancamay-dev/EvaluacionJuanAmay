import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { Cuenta, Pagina } from '../../core/models';
import { CuentasComponent } from './cuentas.component';

const CUENTAS: Cuenta[] = [
  { id: 1, numeroCuenta: '1000000001', titular: 'María López', tipo: 'AHORRO', saldo: 15000, estado: 'ACTIVA', fechaCreacion: '' },
  { id: 2, numeroCuenta: '1000000002', titular: 'Carlos Vega', tipo: 'CORRIENTE', saldo: 800, estado: 'ACTIVA', fechaCreacion: '' },
  { id: 3, numeroCuenta: '1000000003', titular: 'Lucía Paredes', tipo: 'AHORRO', saldo: 2500, estado: 'INACTIVA', fechaCreacion: '' }
];

function pagina(contenido: Cuenta[]): Pagina<Cuenta> {
  return { contenido, pagina: 0, tamano: 100, totalElementos: contenido.length, totalPaginas: 1 };
}

describe('CuentasComponent', () => {
  let fixture: ComponentFixture<CuentasComponent>;
  let component: CuentasComponent;
  let http: HttpTestingController;
  const url = `${environment.apiUrl}/cuentas`;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CuentasComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    fixture = TestBed.createComponent(CuentasComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  async function responder(body: object, opciones?: { status: number; statusText: string }): Promise<void> {
    http.expectOne((r) => r.url === url).flush(body, opciones);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function filas(): HTMLElement[] {
    return Array.from(fixture.nativeElement.querySelectorAll('tr.mat-mdc-row:not(.mat-mdc-no-data-row)'));
  }

  function numerosEnPantalla(): string[] {
    return filas().map((f) => f.querySelector('td')!.textContent!.trim());
  }

  it('muestra una fila por cuenta con número, titular, tipo, saldo y estado', async () => {
    await responder(pagina(CUENTAS));

    expect(filas().length).toBe(3);
    const celdas = Array.from(filas()[0].querySelectorAll('td')).map((c) => c.textContent!.trim());
    expect(celdas).toEqual(['1000000001', 'María López', 'Ahorro', '$15,000.00', 'Activa']);
  });

  it('ordena por saldo ascendente y descendente', async () => {
    await responder(pagina(CUENTAS));

    component.ordenar({ active: 'saldo', direction: 'asc' });
    fixture.detectChanges();
    expect(component.cuentasOrdenadas().map((c) => c.saldo)).toEqual([800, 2500, 15000]);
    expect(numerosEnPantalla()).toEqual(['1000000002', '1000000003', '1000000001']);

    component.ordenar({ active: 'saldo', direction: 'desc' });
    fixture.detectChanges();
    expect(component.cuentasOrdenadas().map((c) => c.saldo)).toEqual([15000, 2500, 800]);
    expect(numerosEnPantalla()).toEqual(['1000000001', '1000000003', '1000000002']);
  });

  it('sin dirección de orden conserva el orden del backend', async () => {
    await responder(pagina(CUENTAS));

    component.ordenar({ active: 'saldo', direction: 'asc' });
    component.ordenar({ active: 'saldo', direction: '' });
    fixture.detectChanges();

    expect(numerosEnPantalla()).toEqual(['1000000001', '1000000002', '1000000003']);
  });

  it('muestra el mensaje de error si falla la carga', async () => {
    await responder(
      { status: 500, codigo: 'ERROR_INTERNO', mensaje: 'Ocurrió un error inesperado. Intente nuevamente.', detalles: [] },
      { status: 500, statusText: 'Server Error' }
    );

    const alerta = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement;
    expect(alerta.textContent).toContain('Ocurrió un error inesperado');
  });

  it('muestra el estado vacío sin cuentas', async () => {
    await responder(pagina([]));

    expect(filas().length).toBe(0);
    expect(fixture.nativeElement.textContent).toContain('No hay cuentas registradas');
  });

  it('cargar() vuelve a consultar el backend', async () => {
    await responder(pagina(CUENTAS));

    component.cargar();
    await responder(pagina(CUENTAS.slice(0, 1)));

    expect(filas().length).toBe(1);
  });
});
