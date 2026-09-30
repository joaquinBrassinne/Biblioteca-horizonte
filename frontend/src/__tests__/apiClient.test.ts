import { describe, it, expect, beforeEach } from 'vitest';
import {
  ApiError,
  setAuthCredentials,
  getAuthCredentials,
  clearAuthCredentials,
} from '../api/apiClient';

describe('ApiClient & ApiError', () => {
  beforeEach(() => {
    clearAuthCredentials();
  });

  it('crea correctamente un ApiError con información de conflicto 409', () => {
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

  it('maneja error 401 Unauthorized cuando la sesión o credenciales no son válidas', () => {
    const errorResponse = {
      status: 401,
      error: 'Unauthorized',
      codigo: 'NO_AUTENTICADO',
      mensaje: 'Se requiere autenticación para acceder a este recurso.',
    };

    const apiError = new ApiError(errorResponse);
    expect(apiError.status).toBe(401);
    expect(apiError.codigo).toBe('NO_AUTENTICADO');
    expect(apiError.message).toContain('autenticación');
  });

  it('maneja error 403 Forbidden cuando el usuario carece de permisos (CP11)', () => {
    const errorResponse = {
      status: 403,
      error: 'Forbidden',
      codigo: 'ACCESO_DENEGADO',
      mensaje: 'No tiene permisos para acceder a esta solicitud.',
    };

    const apiError = new ApiError(errorResponse);
    expect(apiError.status).toBe(403);
    expect(apiError.codigo).toBe('ACCESO_DENEGADO');
    expect(apiError.message).toBe('No tiene permisos para acceder a esta solicitud.');
  });

  it('gestiona correctamente credenciales HTTP Basic en memoria', () => {
    expect(getAuthCredentials()).toBeNull();

    const header = setAuthCredentials('docente1', 'docente123');
    expect(header).toBe(`Basic ${btoa('docente1:docente123')}`);
    expect(getAuthCredentials()).toBe(header);

    clearAuthCredentials();
    expect(getAuthCredentials()).toBeNull();
  });
});
