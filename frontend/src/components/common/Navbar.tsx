import React from 'react';
import { useAuth } from '../../context/AuthContext';

export const Navbar: React.FC = () => {
  const { usuario, logout } = useAuth();

  return (
    <header className="navbar">
      <div className="navbar-container">
        <div className="navbar-brand">
          <div className="navbar-logo">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
          </div>
          <div>
            <h1 className="navbar-title">Biblioteca Horizonte</h1>
            <p className="navbar-subtitle">Gestión de Solicitudes de Reserva de Equipos</p>
          </div>
        </div>

        <div className="navbar-actions">
          {usuario ? (
            <div className="user-session-bar" data-testid="user-session-bar">
              <div className="user-info-chip">
                <span className={`role-badge ${usuario.rol === 'ROLE_BIBLIOTECARIA' ? 'role-biblio' : 'role-docente'}`}>
                  {usuario.rol === 'ROLE_BIBLIOTECARIA' ? 'BIBLIOTECARIA' : 'DOCENTE'}
                </span>
                <span className="user-name" data-testid="authenticated-user-name">
                  {usuario.nombreCompleto}
                </span>
              </div>
              <button
                type="button"
                className="btn btn-sm btn-logout"
                onClick={logout}
                title="Cerrar sesión actual"
                data-testid="logout-btn"
              >
                Cerrar Sesión
              </button>
            </div>
          ) : (
            <div className="navbar-badges">
              <span className="navbar-tag">Sesión no iniciada</span>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

