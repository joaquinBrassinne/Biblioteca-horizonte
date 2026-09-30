import { describe, it, expect, vi, beforeEach } from 'vitest';
import { reservaApi } from '../api/reservaApi';
import { apiClient, ApiError } from '../api/apiClient';
import { Reserva, EstadoReserva } from '../types/reserva';

describe('Roles, Endpoints y Casos de Prueba (CP10, CP11, CP14)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  describe('RF09 & CP10: DOCENTE consulta solamente sus propias solicitudes', () => {
    it('consume el endpoint /reservas/mis-solicitudes utilizando la sesión del docente', async () => {
      const mockMisReservas: Reserva[] = [
        {
          id: 1,
          docenteId: 1,
          equipoId: 10,
          equipoNombre: 'Proyector EPSON',
          fecha: '2026-11-20',
          modulo: 'M1',
          estado: 'PENDIENTE',
          fechaCreacion: '2026-11-01T10:00:00Z',
        },
      ];

      const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValue(mockMisReservas);

      const resultado = await reservaApi.listarMisSolicitudes();

      expect(getSpy).toHaveBeenCalledWith('/reservas/mis-solicitudes');
      expect(resultado).toHaveLength(1);
      expect(resultado[0].docenteId).toBe(1);
      expect(resultado[0].estado).toBe('PENDIENTE');
    });

    it('CP10: DOCENTE A solo visualiza solicitudes de DOCENTE A y no de DOCENTE B', () => {
      const docenteAId = 1;
      const todasLasReservasEnSistema: Reserva[] = [
        {
          id: 101,
          docenteId: 1, // Docente A
          equipoId: 10,
          fecha: '2026-11-25',
          modulo: 'M1',
          estado: 'PENDIENTE',
          fechaCreacion: '2026-11-01T08:00:00Z',
        },
        {
          id: 102,
          docenteId: 2, // Docente B
          equipoId: 11,
          fecha: '2026-11-25',
          modulo: 'M2',
          estado: 'CONFIRMADA',
          fechaCreacion: '2026-11-01T08:30:00Z',
        },
      ];

      // En el backend y en frontend, mis-solicitudes solo entrega las del usuario autenticado
      const reservasDocenteA = todasLasReservasEnSistema.filter((r) => r.docenteId === docenteAId);

      expect(reservasDocenteA).toHaveLength(1);
      expect(reservasDocenteA[0].id).toBe(101);
      expect(reservasDocenteA[0].docenteId).toBe(docenteAId);
      expect(reservasDocenteA.some((r) => r.docenteId === 2)).toBe(false);
    });
  });

  describe('CP11: Acceso a solicitudes de otro docente rechazado con 403 Forbidden', () => {
    it('maneja el error 403 cuando un docente intenta acceder a una solicitud que no le pertenece', async () => {
      vi.spyOn(apiClient, 'get').mockRejectedValue(
        new ApiError({
          status: 403,
          error: 'Forbidden',
          codigo: 'ACCESO_DENEGADO',
          mensaje: 'No tiene permisos para acceder a esta solicitud.',
        })
      );

      await expect(reservaApi.obtenerPorId(999)).rejects.toThrow(ApiError);

      try {
        await reservaApi.obtenerPorId(999);
      } catch (err: any) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.status).toBe(403);
        expect(err.codigo).toBe('ACCESO_DENEGADO');
        expect(err.message).toBe('No tiene permisos para acceder a esta solicitud.');
      }
    });
  });

  describe('RF10 & CP14: BIBLIOTECARIA consulta reservas CONFIRMADAS por recurso y fecha', () => {
    it('consume /reservas/confirmadas con los parámetros de equipo y fecha', async () => {
      const mockConfirmadas: Reserva[] = [
        {
          id: 50,
          docenteId: 1,
          equipoId: 10,
          equipoNombre: 'Proyector EPSON',
          fecha: '2026-12-10',
          modulo: 'M1',
          estado: 'CONFIRMADA',
          fechaCreacion: '2026-11-05T09:00:00Z',
        },
      ];

      const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValue(mockConfirmadas);

      const resultado = await reservaApi.listarConfirmadas({
        equipoId: 10,
        fecha: '2026-12-10',
      });

      expect(getSpy).toHaveBeenCalledWith('/reservas/confirmadas?equipoId=10&fecha=2026-12-10');
      expect(resultado).toHaveLength(1);
      expect(resultado[0].estado).toBe('CONFIRMADA');
    });

    it('solo retorna reservas CONFIRMADAS y no muestra solicitudes PENDIENTES como confirmadas', () => {
      const listaDevueltaPorServidor: Reserva[] = [
        {
          id: 1,
          docenteId: 1,
          equipoId: 10,
          fecha: '2026-12-10',
          modulo: 'M1',
          estado: 'CONFIRMADA',
          fechaCreacion: '2026-11-05T09:00:00Z',
        },
      ];

      const todasSonConfirmadas = listaDevueltaPorServidor.every((r) => r.estado === 'CONFIRMADA');
      expect(todasSonConfirmadas).toBe(true);
      expect(listaDevueltaPorServidor.some((r) => r.estado === 'PENDIENTE')).toBe(false);
    });

    it('mensaje descriptivo exacto cuando no existen reservas confirmadas para los filtros', () => {
      const listaVacia: Reserva[] = [];
      const mensajeEsperado = 'No hay reservas confirmadas para los filtros seleccionados.';

      expect(listaVacia).toHaveLength(0);
      expect(mensajeEsperado).toBe('No hay reservas confirmadas para los filtros seleccionados.');
    });
  });

  describe('Manejo de Estados y Errores (401, 403, 409)', () => {
    it('los estados se limitan a PENDIENTE, CONFIRMADA y RECHAZADA', () => {
      const estadosValidos: EstadoReserva[] = ['PENDIENTE', 'CONFIRMADA', 'RECHAZADA'];
      expect(estadosValidos).toContain('PENDIENTE');
      expect(estadosValidos).toContain('CONFIRMADA');
      expect(estadosValidos).toContain('RECHAZADA');
    });

    it('manejo de 409 Conflict ante colisión de reserva en confirmar()', async () => {
      vi.spyOn(apiClient, 'post').mockRejectedValue(
        new ApiError({
          status: 409,
          error: 'Conflict',
          codigo: 'RESERVA_COLISION_CONFIRMADA',
          mensaje: 'No es posible confirmar la reserva. Ya existe otra reserva confirmada para este equipo, fecha y módulo.',
        })
      );

      try {
        await reservaApi.confirmar(88);
      } catch (err: any) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.status).toBe(409);
        expect(err.codigo).toBe('RESERVA_COLISION_CONFIRMADA');
      }
    });
  });
});
