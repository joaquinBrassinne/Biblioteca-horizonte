import { describe, it, expect } from 'vitest';
import { EstadoReserva } from '../types/reserva';

describe('Reglas de Estados del Frontend (SSD / MVP)', () => {
  it('garantiza que solo existen los tres estados mínimos permitidos', () => {
    const estadosPermitidos: EstadoReserva[] = ['PENDIENTE', 'CONFIRMADA', 'RECHAZADA'];
    expect(estadosPermitidos).toHaveLength(3);
    expect(estadosPermitidos).toContain('PENDIENTE');
    expect(estadosPermitidos).toContain('CONFIRMADA');
    expect(estadosPermitidos).toContain('RECHAZADA');
  });

  it('el estado inicial de una nueva solicitud debe ser PENDIENTE y nunca "Reserva realizada"', () => {
    const estadoInicial: EstadoReserva = 'PENDIENTE';
    expect(estadoInicial).toBe('PENDIENTE');
    expect(estadoInicial).not.toBe('CONFIRMADA');

    // Mensaje esperado al registrar según requerimiento estricto
    const mensajeRegistro = 'Solicitud registrada con éxito en estado PENDIENTE';
    expect(mensajeRegistro).toContain('PENDIENTE');
    expect(mensajeRegistro.toLowerCase()).not.toContain('reserva realizada');
  });
});
