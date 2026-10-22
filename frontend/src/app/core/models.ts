export type TipoCuenta = 'AHORRO' | 'CORRIENTE';
export type EstadoCuenta = 'ACTIVA' | 'INACTIVA';
export type EstadoTransferencia = 'COMPLETADA' | 'RECHAZADA' | 'EN_REVISION';

export interface Cuenta {
  id: number;
  numeroCuenta: string;
  titular: string;
  tipo: TipoCuenta;
  saldo: number;
  estado: EstadoCuenta;
  fechaCreacion: string;
}

export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface TransferenciaRequest {
  cuentaOrigen: string;
  cuentaDestino: string;
  monto: number;
}

export interface Transferencia {
  id: number;
  cuentaOrigen: string;
  cuentaDestino: string;
  monto: number;
  estado: EstadoTransferencia;
  motivo: string | null;
  fecha: string;
}

/** Formato de error uniforme del backend. */
export interface ErrorApi {
  timestamp: string;
  status: number;
  codigo: string;
  mensaje: string;
  detalles: string[];
}
