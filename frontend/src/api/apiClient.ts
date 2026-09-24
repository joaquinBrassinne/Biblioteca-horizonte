import { ApiErrorResponse } from '../types/reserva';

const API_BASE_URL = import.meta.env.VITE_API_URL || '/api/v1';

export class ApiError extends Error {
  status: number;
  codigo?: string;
  validaciones?: Record<string, string> | null;

  constructor(errorResponse: ApiErrorResponse) {
    super(errorResponse.mensaje || errorResponse.error || 'Error en la petición');
    this.name = 'ApiError';
    this.status = errorResponse.status;
    this.codigo = errorResponse.codigo;
    this.validaciones = errorResponse.validaciones;
  }
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const url = `${API_BASE_URL}${endpoint}`;
  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    ...options.headers,
  };

  try {
    const response = await fetch(url, {
      ...options,
      headers,
    });

    // En caso de error HTTP (4xx, 5xx)
    if (!response.ok) {
      let errorBody: ApiErrorResponse;
      try {
        errorBody = await response.json();
      } catch {
        errorBody = {
          status: response.status,
          error: response.statusText,
          mensaje: `Error en la solicitud (${response.status}: ${response.statusText})`,
        };
      }
      throw new ApiError(errorBody);
    }

    // Si la respuesta no tiene contenido (ej. 204)
    if (response.status === 204) {
      return {} as T;
    }

    return await response.json();
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    // Error de red / conexión
    throw new ApiError({
      status: 0,
      error: 'Network Error',
      mensaje: 'No se pudo conectar con el servidor. Verifique que el backend esté en ejecución.',
    });
  }
}

export const apiClient = {
  get: <T>(endpoint: string) => request<T>(endpoint, { method: 'GET' }),
  post: <T>(endpoint: string, body?: unknown) =>
    request<T>(endpoint, {
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined,
    }),
};
