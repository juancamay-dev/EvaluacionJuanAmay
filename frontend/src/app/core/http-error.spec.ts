import { HttpErrorResponse } from '@angular/common/http';

import { extraerMensajeError } from './http-error';

describe('extraerMensajeError', () => {
  it('sin conexión (status 0) devuelve el mensaje de conexión', () => {
    const error = new HttpErrorResponse({ status: 0 });

    expect(extraerMensajeError(error)).toContain('No se pudo conectar con el servidor');
  });

  it('con cuerpo del backend devuelve su mensaje', () => {
    const error = new HttpErrorResponse({
      status: 422,
      error: {
        codigo: 'SALDO_INSUFICIENTE',
        mensaje: 'Saldo insuficiente en la cuenta 1000000001',
        detalles: ['transferenciaId: 15']
      }
    });

    expect(extraerMensajeError(error)).toBe('Saldo insuficiente en la cuenta 1000000001');
  });

  it('con detalles de validación los agrega al mensaje', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: {
        codigo: 'VALIDACION',
        mensaje: 'La solicitud contiene datos inválidos',
        detalles: ['monto: debe ser mayor a cero', 'cuentaOrigen: es obligatoria']
      }
    });

    expect(extraerMensajeError(error)).toBe(
      'La solicitud contiene datos inválidos: monto: debe ser mayor a cero, cuentaOrigen: es obligatoria'
    );
  });

  it('sin cuerpo devuelve un error inesperado con el status', () => {
    const error = new HttpErrorResponse({ status: 500 });

    expect(extraerMensajeError(error)).toBe('Error inesperado (500)');
  });
});
