import React, { createContext, useContext, useState, useEffect } from 'react';
import { UsuarioAutenticado, RolUsuario } from '../types/reserva';
import { setAuthCredentials, clearAuthCredentials, getAuthCredentials, apiClient } from '../api/apiClient';

interface AuthContextType {
  usuario: UsuarioAutenticado | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<UsuarioAutenticado>;
  logout: () => void;
  cambiarUsuarioRapido: (perfil: 'docente1' | 'docente2' | 'bibliotecaria') => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const PERFILES_DEV: Record<string, { nombre: string; rol: RolUsuario; docenteId?: number; pass: string }> = {
  docente1: {
    nombre: 'Prof. Juan Pérez (Docente 1)',
    rol: 'ROLE_DOCENTE',
    docenteId: 1,
    pass: 'docente123',
  },
  docente2: {
    nombre: 'Prof. María González (Docente 2)',
    rol: 'ROLE_DOCENTE',
    docenteId: 2,
    pass: 'docente123',
  },
  docente: {
    nombre: 'Prof. Juan Pérez (Docente)',
    rol: 'ROLE_DOCENTE',
    docenteId: 1,
    pass: 'docente123',
  },
  bibliotecaria: {
    nombre: 'Lucía (Bibliotecaria)',
    rol: 'ROLE_BIBLIOTECARIA',
    pass: 'biblio123',
  },
};

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [usuario, setUsuario] = useState<UsuarioAutenticado | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    // Restaurar sesión previa si existe en sessionStorage
    try {
      const savedUser = sessionStorage.getItem('bh_user_profile');
      const savedToken = getAuthCredentials();
      if (savedUser && savedToken) {
        setUsuario(JSON.parse(savedUser));
      }
    } catch {
      // Ignorar errores de parseo
    } finally {
      setLoading(false);
    }
  }, []);

  const login = async (username: string, password: string): Promise<UsuarioAutenticado> => {
    // 1. Configurar credenciales HTTP Basic
    setAuthCredentials(username, password);

    try {
      // 2. Probar conectividad y credenciales contra el backend
      // El backend requiere autenticación en /reservas
      await apiClient.get('/reservas');

      // 3. Determinar rol y metadatos
      const lowerUser = username.trim().toLowerCase();
      let rol: RolUsuario = 'ROLE_DOCENTE';
      let nombreCompleto = `Usuario (${username})`;
      let docenteId: number | undefined;

      if (lowerUser === 'bibliotecaria') {
        rol = 'ROLE_BIBLIOTECARIA';
        nombreCompleto = 'Lucía (Bibliotecaria)';
      } else if (lowerUser === 'docente1' || lowerUser === 'docente') {
        rol = 'ROLE_DOCENTE';
        nombreCompleto = 'Prof. Juan Pérez';
        docenteId = 1;
      } else if (lowerUser === 'docente2') {
        rol = 'ROLE_DOCENTE';
        nombreCompleto = 'Prof. María González';
        docenteId = 2;
      } else if (lowerUser.startsWith('docente')) {
        rol = 'ROLE_DOCENTE';
        const matches = lowerUser.match(/\d+$/);
        docenteId = matches ? Number(matches[0]) : 1;
        nombreCompleto = `Docente #${docenteId}`;
      }

      const perfil: UsuarioAutenticado = {
        username,
        nombreCompleto,
        rol,
        docenteId,
      };

      setUsuario(perfil);
      try {
        sessionStorage.setItem('bh_user_profile', JSON.stringify(perfil));
      } catch {
        // Ignorar
      }
      return perfil;
    } catch (err) {
      clearAuthCredentials();
      setUsuario(null);
      throw err;
    }
  };

  const logout = (): void => {
    clearAuthCredentials();
    setUsuario(null);
    try {
      sessionStorage.removeItem('bh_user_profile');
    } catch {
      // Ignorar
    }
  };

  const cambiarUsuarioRapido = async (tipo: 'docente1' | 'docente2' | 'bibliotecaria'): Promise<void> => {
    const p = PERFILES_DEV[tipo];
    if (p) {
      await login(tipo, p.pass);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        usuario,
        loading,
        login,
        logout,
        cambiarUsuarioRapido,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth debe ser utilizado dentro de un AuthProvider');
  }
  return context;
};
