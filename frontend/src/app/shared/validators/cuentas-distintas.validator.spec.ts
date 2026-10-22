import { FormControl, FormGroup } from '@angular/forms';

import { cuentasDistintasValidator } from './cuentas-distintas.validator';

function grupo(origen: string, destino: string): FormGroup {
  return new FormGroup({ cuentaOrigen: new FormControl(origen), cuentaDestino: new FormControl(destino) });
}

describe('cuentasDistintasValidator', () => {
  it('cuentas iguales devuelven el error cuentasIguales', () => {
    expect(cuentasDistintasValidator(grupo('1000000001', '1000000001'))).toEqual({ cuentasIguales: true });
  });

  it('cuentas distintas son válidas', () => {
    expect(cuentasDistintasValidator(grupo('1000000001', '1000000002'))).toBeNull();
  });

  it('si alguna está vacía no reporta error (lo hace required)', () => {
    expect(cuentasDistintasValidator(grupo('', '1000000002'))).toBeNull();
    expect(cuentasDistintasValidator(grupo('1000000001', ''))).toBeNull();
  });
});
