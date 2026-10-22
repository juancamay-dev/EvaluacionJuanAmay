import { HttpErrorResponse } from '@angular/common/http';

import { ErrorApi } from './models';

/** Traduce cualquier error HTTP a un mensaje legible para el usuario. */
export function extraerMensajeError(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'Error inesperado';
  }
  if (error.status === 0) {
    return 'No se pudo conectar con el servidor. Verifica que el backend esté en ejecución.';
  }
  const cuerpo = error.error as Partial<ErrorApi> | null;
  if (cuerpo && typeof cuerpo === 'object' && cuerpo.mensaje) {
    // Solo las validaciones traen detalles útiles para el usuario ("campo: mensaje")
    const detalles = cuerpo.codigo === 'VALIDACION' ? (cuerpo.detalles ?? []) : [];
    return detalles.length > 0 ? `${cuerpo.mensaje}: ${detalles.join(', ')}` : cuerpo.mensaje;
  }
  return `Error inesperado (${error.status})`;
}
