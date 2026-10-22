import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { Cuenta, Transferencia } from '../../core/models';
import { TransferenciaFormComponent } from './transferencia-form.component';

const CUENTAS: Cuenta[] = [
  { id: 1, numeroCuenta: '1000000001', titular: 'María López', tipo: 'AHORRO', saldo: 15000, estado: 'ACTIVA', fechaCreacion: '' },
  { id: 2, numeroCuenta: '1000000002', titular: 'Carlos Vega', tipo: 'CORRIENTE', saldo: 8000, estado: 'ACTIVA', fechaCreacion: '' }
];

describe('TransferenciaFormComponent', () => {
  let fixture: ComponentFixture<TransferenciaFormComponent>;
  let component: TransferenciaFormComponent;
  let http: HttpTestingController;
  const urlCuentas = `${environment.apiUrl}/cuentas`;
  const urlTransferencias = `${environment.apiUrl}/transferencias`;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransferenciaFormComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    fixture = TestBed.createComponent(TransferenciaFormComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    responderCuentas();
  });

  afterEach(() => http.verify());

  function responderCuentas(): void {
    http.expectOne((r) => r.url === urlCuentas).flush({
      contenido: CUENTAS, pagina: 0, tamano: 100, totalElementos: 2, totalPaginas: 1
    });
  }

  function texto(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  function enviarFormulario(): void {
    (fixture.nativeElement as HTMLElement).querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  function llenarValido(): void {
    component.form.setValue({ cuentaOrigen: '1000000001', cuentaDestino: '1000000002', monto: 250.5 });
  }

  function transferencia(parcial: Partial<Transferencia> = {}): Transferencia {
    return {
      id: 9, cuentaOrigen: '1000000001', cuentaDestino: '1000000002', monto: 250.5,
      estado: 'COMPLETADA', motivo: null, fecha: '2026-10-21T15:00:00Z', ...parcial
    };
  }

  async function responderTransferencia(body: object, opciones?: { status: number; statusText: string }) {
    http.expectOne(urlTransferencias).flush(body, opciones);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('el formulario vacío es inválido', () => {
    expect(component.form.invalid).toBeTrue();
    expect(component.form.controls.cuentaOrigen.hasError('required')).toBeTrue();
    expect(component.form.controls.cuentaDestino.hasError('required')).toBeTrue();
    expect(component.form.controls.monto.hasError('required')).toBeTrue();
  });

  it('monto cero, negativo o con tres decimales es inválido', () => {
    const monto = component.form.controls.monto;

    monto.setValue(0);
    expect(monto.hasError('min')).toBeTrue();
    monto.setValue(-5);
    expect(monto.hasError('min')).toBeTrue();
    monto.setValue(10.555);
    expect(monto.hasError('pattern')).toBeTrue();
    monto.setValue(10.55);
    expect(monto.valid).toBeTrue();
  });

  it('cuentas iguales invalidan el formulario y muestran el aviso', () => {
    component.form.setValue({ cuentaOrigen: '1000000001', cuentaDestino: '1000000001', monto: 10 });
    component.form.markAllAsTouched();
    fixture.detectChanges();

    expect(component.form.hasError('cuentasIguales')).toBeTrue();
    expect(texto()).toContain('deben ser diferentes');
  });

  it('enviar un formulario inválido no llama al backend', () => {
    enviarFormulario();

    http.expectNone(urlTransferencias);
    expect(component.form.controls.cuentaOrigen.touched).toBeTrue();
    expect(component.form.controls.monto.touched).toBeTrue();
    expect(texto()).toContain('Ingresa el monto');
  });

  it('enviar uno válido hace POST con el cuerpo correcto', () => {
    llenarValido();
    enviarFormulario();

    const req = http.expectOne(urlTransferencias);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ cuentaOrigen: '1000000001', cuentaDestino: '1000000002', monto: 250.5 });
    req.flush(transferencia());
    responderCuentas();
  });

  it('éxito muestra mensaje, reinicia el formulario y emite transferenciaRealizada', async () => {
    const emitidas: Transferencia[] = [];
    component.transferenciaRealizada.subscribe((t) => emitidas.push(t));
    llenarValido();
    enviarFormulario();

    await responderTransferencia(transferencia());
    responderCuentas();

    expect(component.mensaje()?.tipo).toBe('exito');
    expect(texto()).toContain('Transferencia #9 por $250.50 realizada');
    expect(component.form.getRawValue()).toEqual({ cuentaOrigen: '', cuentaDestino: '', monto: null });
    expect(component.form.untouched).toBeTrue();
    expect((fixture.nativeElement as HTMLElement).querySelector('mat-error')).toBeNull();
    expect(emitidas.map((t) => t.id)).toEqual([9]);
  });

  it('EN_REVISION muestra un aviso con el motivo', async () => {
    llenarValido();
    enviarFormulario();

    await responderTransferencia(transferencia({ estado: 'EN_REVISION', motivo: 'Monto superior a $5,000.00' }));
    responderCuentas();

    const aviso = (fixture.nativeElement as HTMLElement).querySelector('.alerta-aviso');
    expect(aviso?.textContent).toContain('marcada para revisión: Monto superior a $5,000.00');
  });

  it('un 422 muestra el mensaje del backend', async () => {
    llenarValido();
    enviarFormulario();

    await responderTransferencia(
      { status: 422, codigo: 'LIMITE_DIARIO_EXCEDIDO', detalles: ['transferenciaId: 4'],
        mensaje: 'La transferencia supera el límite diario de $10,000.00. Monto disponible hoy: $1,000.00' },
      { status: 422, statusText: 'Unprocessable Entity' }
    );

    const alerta = (fixture.nativeElement as HTMLElement).querySelector('.alerta-error');
    expect(alerta?.textContent?.trim()).toBe(
      'La transferencia supera el límite diario de $10,000.00. Monto disponible hoy: $1,000.00'
    );
    expect(component.form.controls.monto.value).toBe(250.5);
  });

  it('el botón se deshabilita mientras se procesa', async () => {
    llenarValido();
    enviarFormulario();

    const boton = (fixture.nativeElement as HTMLElement).querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(component.enviando()).toBeTrue();
    expect(boton.disabled).toBeTrue();

    await responderTransferencia(transferencia());
    responderCuentas();
    expect(component.enviando()).toBeFalse();
    expect(boton.disabled).toBeFalse();
  });
});
