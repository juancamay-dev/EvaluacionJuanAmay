import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { environment } from '../environments/environment';
import { Transferencia } from './core/models';
import { TransferenciaFormComponent } from './features/transferencia-form/transferencia-form.component';
import { App } from './app';

describe('App', () => {
  let fixture: ComponentFixture<App>;
  let http: HttpTestingController;
  const urlCuentas = `${environment.apiUrl}/cuentas`;
  const paginaVacia = { contenido: [], pagina: 0, tamano: 100, totalElementos: 0, totalPaginas: 0 };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    fixture = TestBed.createComponent(App);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    // El formulario y el dashboard cargan las cuentas al iniciar
    http.match((r) => r.url === urlCuentas).forEach((req) => req.flush(paginaVacia));
  });

  afterEach(() => http.verify());

  it('renderiza el formulario y el dashboard', () => {
    const elemento = fixture.nativeElement as HTMLElement;

    expect(elemento.querySelector('mat-toolbar')?.textContent).toContain('Transferencias Interbancarias');
    expect(elemento.querySelector('app-transferencia-form')).not.toBeNull();
    expect(elemento.querySelector('app-cuentas')).not.toBeNull();
  });

  it('al emitirse transferenciaRealizada recarga las cuentas', () => {
    const formulario = fixture.debugElement.query(By.directive(TransferenciaFormComponent))
      .componentInstance as TransferenciaFormComponent;
    const transferencia: Transferencia = {
      id: 1, cuentaOrigen: '1000000001', cuentaDestino: '1000000002', monto: 10,
      estado: 'COMPLETADA', motivo: null, fecha: '2026-10-21T15:00:00Z'
    };

    formulario.transferenciaRealizada.emit(transferencia);

    http.expectOne((r) => r.url === urlCuentas).flush(paginaVacia);
  });
});
