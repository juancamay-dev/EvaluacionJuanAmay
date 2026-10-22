import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Validador de grupo: la cuenta origen debe ser distinta de la cuenta destino. */
export const cuentasDistintasValidator: ValidatorFn = (grupo: AbstractControl): ValidationErrors | null => {
  const origen = grupo.get('cuentaOrigen')?.value;
  const destino = grupo.get('cuentaDestino')?.value;
  return origen && destino && origen === destino ? { cuentasIguales: true } : null;
};
