import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { ApiError } from '../../api/apiClient';
import { Alert } from '../common/Alert';

export const LoginForm: React.FC = () => {
  const { login, cambiarUsuarioRapido } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password) {
      setErrorMsg('Por favor complete usuario y contraseña.');
      return;
    }
    setErrorMsg(null);
    setLoading(true);
    try {
      await login(username.trim(), password);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 401) {
          setErrorMsg('Error de Autenticación (401): Credenciales incorrectas. Verifique usuario y contraseña.');
        } else {
          setErrorMsg(err.message || 'Error al iniciar sesión.');
        }
      } else {
        setErrorMsg('Error de conexión con el backend.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleQuickLogin = async (tipo: 'docente1' | 'docente2' | 'bibliotecaria') => {
    setErrorMsg(null);
    setLoading(true);
    try {
      await cambiarUsuarioRapido(tipo);
    } catch (err) {
      if (err instanceof ApiError) {
        setErrorMsg(err.message || 'Error al autenticar usuario.');
      } else {
        setErrorMsg('Error de comunicación con el backend.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card login-card" data-testid="login-container">
      <div className="card-header text-center">
        <div className="login-icon-badge">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="28" height="28">
            <path strokeLinecap="round" strokeLinejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
          </svg>
        </div>
        <h2 className="card-title">Identificación de Usuario</h2>
        <p className="card-description">
          Seleccione su perfil de acceso o ingrese sus credenciales institucionales para gestionar las reservas.
        </p>
      </div>

      {errorMsg && (
        <Alert
          type="error"
          title="Fallo de Autenticación"
          message={errorMsg}
          onClose={() => setErrorMsg(null)}
        />
      )}

      {/* Acceso Rápido para Pruebas / Demostración */}
      <div className="quick-access-box">
        <span className="quick-access-title">Acceso rápido por perfil:</span>
        <div className="quick-buttons-grid">
          <button
            type="button"
            className="btn btn-outline quick-btn"
            onClick={() => handleQuickLogin('docente1')}
            disabled={loading}
            data-testid="quick-login-docente1"
          >
            <span className="quick-role-tag tag-docente">DOCENTE 1</span>
            <span className="quick-name">Prof. Juan Pérez</span>
            <span className="quick-hint">docente1</span>
          </button>

          <button
            type="button"
            className="btn btn-outline quick-btn"
            onClick={() => handleQuickLogin('docente2')}
            disabled={loading}
            data-testid="quick-login-docente2"
          >
            <span className="quick-role-tag tag-docente">DOCENTE 2</span>
            <span className="quick-name">Prof. María González</span>
            <span className="quick-hint">docente2</span>
          </button>

          <button
            type="button"
            className="btn btn-outline quick-btn"
            onClick={() => handleQuickLogin('bibliotecaria')}
            disabled={loading}
            data-testid="quick-login-bibliotecaria"
          >
            <span className="quick-role-tag tag-biblio">BIBLIOTECARIA</span>
            <span className="quick-name">Lucía</span>
            <span className="quick-hint">bibliotecaria</span>
          </button>
        </div>
      </div>

      <div className="login-divider">
        <span>O ingrese manualmente</span>
      </div>

      {/* Formulario Estándar */}
      <form onSubmit={handleSubmit} className="login-form" noValidate>
        <div className="form-group">
          <label htmlFor="login-username" className="form-label">
            Usuario <span className="required">*</span>
          </label>
          <input
            id="login-username"
            type="text"
            className="form-input"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Ej: docente1, docente2 o bibliotecaria"
            required
            disabled={loading}
            data-testid="login-username-input"
          />
        </div>

        <div className="form-group">
          <label htmlFor="login-password" className="form-label">
            Contraseña <span className="required">*</span>
          </label>
          <input
            id="login-password"
            type="password"
            className="form-input"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            required
            disabled={loading}
            data-testid="login-password-input"
          />
        </div>

        <button
          type="submit"
          className="btn btn-primary btn-block"
          disabled={loading}
          data-testid="login-submit-btn"
        >
          {loading ? (
            <>
              <span className="spinner"></span> Autenticando...
            </>
          ) : (
            'Ingresar al Sistema'
          )}
        </button>
      </form>
    </div>
  );
};
