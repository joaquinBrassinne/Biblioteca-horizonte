import { apiClient } from './apiClient';
import { Reserva, CrearReservaDTO, FiltroConfirmadasDTO } from '../types/reserva';

export const reservaApi = {
  /**
   * Crea una solicitud de reserva.
   * La respuesta del backend siempre tendrá estado PENDIENTE inicialmente.
   */
  crear: (dto: CrearReservaDTO): Promise<Reserva> => {
    return apiClient.post<Reserva>('/reservas', dto);
  },

  /**
   * Obtiene la lista completa de solicitudes de reserva (para perfil BIBLIOTECARIA).
   */
  listar: (): Promise<Reserva[]> => {
    return apiClient.get<Reserva[]>('/reservas');
  },

  /**
   * RF09 / CP10: Obtiene exclusivamente las solicitudes pertenecientes al DOCENTE autenticado.
   * Consume /api/v1/reservas/mis-solicitudes utilizando la sesión del usuario.
   */
  listarMisSolicitudes: (): Promise<Reserva[]> => {
    return apiClient.get<Reserva[]>('/reservas/mis-solicitudes');
  },

  /**
   * RF10 / CP14: Consulta exclusivamente reservas en estado CONFIRMADA filtrando por recurso y/o fecha.
   * Consume /api/v1/reservas/confirmadas?equipoId=...&fecha=...
   */
  listarConfirmadas: (filtros?: FiltroConfirmadasDTO): Promise<Reserva[]> => {
    const params = new URLSearchParams();
    if (filtros?.equipoId) {
      params.append('equipoId', String(filtros.equipoId));
    }
    if (filtros?.fecha) {
      params.append('fecha', filtros.fecha);
    }
    const query = params.toString();
    return apiClient.get<Reserva[]>(`/reservas/confirmadas${query ? `?${query}` : ''}`);
  },

  /**
   * Obtiene una solicitud puntual por identificador.
   * En caso de que un DOCENTE intente acceder a una solicitud ajena, el backend retornará 403 Forbidden (CP11).
   */
  obtenerPorId: (id: number): Promise<Reserva> => {
    return apiClient.get<Reserva>(`/reservas/${id}`);
  },

  /**
   * Confirma una solicitud en estado PENDIENTE.
   * Si ya existe una reserva confirmada para el mismo equipo, fecha y módulo,
   * el backend responderá con error HTTP 409 Conflict (RN-001).
   */
  confirmar: (id: number): Promise<Reserva> => {
    return apiClient.post<Reserva>(`/reservas/${id}/confirmar`);
  },

  /**
   * Rechaza una solicitud en estado PENDIENTE.
   */
  rechazar: (id: number): Promise<Reserva> => {
    return apiClient.post<Reserva>(`/reservas/${id}/rechazar`);
  },
};

