export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'RECHAZADA';

export interface Reserva {
  id: number;
  docenteId: number;
  equipoId: number;
  equipoNombre?: string;
  fecha: string;
  modulo: string;
  estado: EstadoReserva;
  fechaCreacion: string;
}

export interface CrearReservaDTO {
  docenteId: number;
  equipoId: number;
  fecha: string;
  modulo: string;
}

export interface Equipo {
  id: number;
  nombre: string;
}

export interface ApiErrorResponse {
  timestamp?: string;
  status: number;
  error: string;
  codigo?: string;
  mensaje: string;
  path?: string;
  validaciones?: Record<string, string> | null;
}
