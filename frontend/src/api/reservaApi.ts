import { apiClient } from './apiClient';
import { Reserva, CrearReservaDTO } from '../types/reserva';

export const reservaApi = {
  /**
   * Crea una solicitud de reserva.
   * La respuesta del backend siempre tendrá estado PENDIENTE inicialmente.
   */
  crear: (dto: CrearReservaDTO): Promise<Reserva> => {
    return apiClient.post<Reserva>('/reservas', dto);
  },

  /**
   * Obtiene la lista completa de solicitudes de reserva.
   */
  listar: (): Promise<Reserva[]> => {
    return apiClient.get<Reserva[]>('/reservas');
  },

  /**
   * Obtiene una solicitud puntual por identificador.
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
