import { describe, it, expect } from 'vitest';
import { ApiError } from '../api/apiClient';

describe('ApiClient & ApiError', () => {
  it('crea correctamente un ApiError con información de respuesta de error', () => {
    const errorResponse = {
      status: 409,
      error: 'Conflict',
      codigo: 'RESERVA_COLISION_CONFIRMADA',
      mensaje: 'Ya existe una reserva confirmada para este equipo, fecha y módulo.',
      validaciones: null,
    };

    const apiError = new ApiError(errorResponse);
    expect(apiError.name).toBe('ApiError');
    expect(apiError.status).toBe(409);
    expect(apiError.codigo).toBe('RESERVA_COLISION_CONFIRMADA');
    expect(apiError.message).toBe('Ya existe una reserva confirmada para este equipo, fecha y módulo.');
  });

  it('maneja errores de validación 400 con mapa de validaciones', () => {
    const errorResponse = {
      status: 400,
      error: 'Bad Request',
      codigo: 'DATOS_INVALIDOS',
      mensaje: 'Uno o más campos presentan errores de validación.',
      validaciones: {
        docenteId: 'El identificador del docente es obligatorio',
        fecha: 'La fecha de reserva es obligatoria',
      },
    };

    const apiError = new ApiError(errorResponse);
    expect(apiError.status).toBe(400);
    expect(apiError.validaciones?.docenteId).toBe('El identificador del docente es obligatorio');
    expect(apiError.validaciones?.fecha).toBe('La fecha de reserva es obligatoria');
  });
});
