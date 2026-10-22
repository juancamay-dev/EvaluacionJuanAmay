import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, output, signal, viewChild } from '@angular/core';
import { FormGroupDirective, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import { extraerMensajeError } from '../../core/http-error';
import { Cuenta, Transferencia } from '../../core/models';
import { CuentaService } from '../../core/services/cuenta.service';
import { TransferenciaService } from '../../core/services/transferencia.service';
import { cuentasDistintasValidator } from '../../shared/validators/cuentas-distintas.validator';

export interface MensajeFormulario {
  tipo: 'exito' | 'aviso' | 'error';
  texto: string;
}

const MONTO_CON_DOS_DECIMALES = /^\d+(\.\d{1,2})?$/;

@Component({
  selector: 'app-transferencia-form',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule
  ],
  providers: [CurrencyPipe],
  templateUrl: './transferencia-form.component.html',
  styleUrl: './transferencia-form.component.scss'
})
export class TransferenciaFormComponent implements OnInit {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly cuentaService = inject(CuentaService);
  private readonly transferenciaService = inject(TransferenciaService);
  private readonly currency = inject(CurrencyPipe);
  private readonly formDirective = viewChild.required(FormGroupDirective);

  readonly transferenciaRealizada = output<Transferencia>();

  readonly cuentas = signal<Cuenta[]>([]);
  readonly enviando = signal(false);
  readonly mensaje = signal<MensajeFormulario | null>(null);

  readonly form = this.fb.group(
    {
      cuentaOrigen: ['', Validators.required],
      cuentaDestino: ['', Validators.required],
      monto: this.fb.control<number | null>(null, [
        Validators.required,
        Validators.min(0.01),
        Validators.pattern(MONTO_CON_DOS_DECIMALES)
      ])
    },
    { validators: cuentasDistintasValidator }
  );

  ngOnInit(): void {
    this.cargarCuentas();
  }

  /** Las inactivas se listan (con etiqueta): el rechazo lo decide el backend. */
  cargarCuentas(): void {
    this.cuentaService.listar().subscribe({
      next: (pagina) => this.cuentas.set(pagina.contenido),
      error: (err: unknown) => this.mensaje.set({ tipo: 'error', texto: extraerMensajeError(err) })
    });
  }

  enviar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { cuentaOrigen, cuentaDestino, monto } = this.form.getRawValue();
    this.enviando.set(true);
    this.mensaje.set(null);
    this.transferenciaService.ejecutar({ cuentaOrigen, cuentaDestino, monto: Number(monto) }).subscribe({
      next: (transferencia) => {
        this.enviando.set(false);
        this.mensaje.set(this.mensajeDeExito(transferencia));
        // resetForm() también limpia el estado submitted; form.reset() dejaría los campos en rojo
        this.formDirective().resetForm();
        this.transferenciaRealizada.emit(transferencia);
        this.cargarCuentas();
      },
      error: (err: unknown) => {
        this.enviando.set(false);
        this.mensaje.set({ tipo: 'error', texto: extraerMensajeError(err) });
      }
    });
  }

  private mensajeDeExito(transferencia: Transferencia): MensajeFormulario {
    const monto = this.currency.transform(transferencia.monto, 'USD');
    if (transferencia.estado === 'EN_REVISION') {
      return {
        tipo: 'aviso',
        texto: `Transferencia #${transferencia.id} por ${monto} ejecutada y marcada para revisión: ${transferencia.motivo}`
      };
    }
    return {
      tipo: 'exito',
      texto: `Transferencia #${transferencia.id} por ${monto} realizada de ${transferencia.cuentaOrigen} a ${transferencia.cuentaDestino}`
    };
  }
}
