import { CurrencyPipe, TitleCasePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';

import { extraerMensajeError } from '../../core/http-error';
import { Cuenta } from '../../core/models';
import { CuentaService } from '../../core/services/cuenta.service';

@Component({
  selector: 'app-cuentas',
  imports: [
    CurrencyPipe,
    TitleCasePipe,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
    MatSortModule,
    MatTableModule
  ],
  templateUrl: './cuentas.component.html',
  styleUrl: './cuentas.component.scss'
})
export class CuentasComponent implements OnInit {
  private readonly cuentaService = inject(CuentaService);

  readonly columnas = ['numeroCuenta', 'titular', 'tipo', 'saldo', 'estado'];
  readonly cuentas = signal<Cuenta[]>([]);
  readonly cargando = signal(false);
  readonly error = signal<string | null>(null);
  readonly orden = signal<Sort>({ active: '', direction: '' });

  /** Sin dirección conserva el orden del backend (por número de cuenta). */
  readonly cuentasOrdenadas = computed(() => {
    const { active, direction } = this.orden();
    const cuentas = this.cuentas();
    if (active !== 'saldo' || direction === '') {
      return cuentas;
    }
    const factor = direction === 'asc' ? 1 : -1;
    return [...cuentas].sort((a, b) => (a.saldo - b.saldo) * factor);
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.cuentaService.listar().subscribe({
      next: (pagina) => {
        this.cuentas.set(pagina.contenido);
        this.cargando.set(false);
      },
      error: (err: unknown) => {
        this.error.set(extraerMensajeError(err));
        this.cargando.set(false);
      }
    });
  }

  ordenar(orden: Sort): void {
    this.orden.set(orden);
  }
}
