import { ApiErrorResponse } from '../types/reserva';

const API_BASE_URL = import.meta.env.VITE_API_URL || '/api/v1';

let currentAuthHeader: string | null = null;

export const setAuthCredentials = (username: string, password: string): string => {
  const token = btoa(`${username}:${password}`);
  currentAuthHeader = `Basic ${token}`;
  try {
    sessionStorage.setItem('bh_auth_token', currentAuthHeader);
    sessionStorage.setItem('bh_auth_user', username);
  } catch {
    // Entorno sin sessionStorage (ej. tests)
  }
  return currentAuthHeader;
};

export const clearAuthCredentials = (): void => {
  currentAuthHeader = null;
  try {
    sessionStorage.removeItem('bh_auth_token');
    sessionStorage.removeItem('bh_auth_user');
  } catch {
    // Ignorar si no está disponible
  }
};

export const getAuthCredentials = (): string | null => {
  if (!currentAuthHeader) {
    try {
      currentAuthHeader = sessionStorage.getItem('bh_auth_token');
    } catch {
      // Ignorar
    }
  }
  return currentAuthHeader;
};

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
  const auth = getAuthCredentials();

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    ...(auth ? { Authorization: auth } : {}),
    ...((options.headers as Record<string, string>) || {}),
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

      // Tratamiento específico de mensajes claros según código HTTP
      if (response.status === 401) {
        errorBody.mensaje =
          errorBody.mensaje || 'La sesión o autenticación no es válida. Inicie sesión con credenciales correctas.';
      } else if (response.status === 403) {
        errorBody.mensaje =
          errorBody.mensaje || 'No tiene permisos para realizar esta operación o acceder a esta información.';
      } else if (response.status === 409) {
        errorBody.mensaje =
          errorBody.mensaje || 'Conflicto de reserva: el slot ya está confirmado o la transición de estado no es válida.';
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

