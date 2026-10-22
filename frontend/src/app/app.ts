import { Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';

import { CuentasComponent } from './features/cuentas/cuentas.component';
import { TransferenciaFormComponent } from './features/transferencia-form/transferencia-form.component';

@Component({
  selector: 'app-root',
  imports: [MatIconModule, MatToolbarModule, CuentasComponent, TransferenciaFormComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {}
