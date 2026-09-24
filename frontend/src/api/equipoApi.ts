import { apiClient } from './apiClient';
import { Equipo } from '../types/reserva';

export const equipoApi = {
  /**
   * Obtiene el catálogo de equipos disponibles para el selector del formulario.
   */
  listar: (): Promise<Equipo[]> => {
    return apiClient.get<Equipo[]>('/equipos');
  },
};
