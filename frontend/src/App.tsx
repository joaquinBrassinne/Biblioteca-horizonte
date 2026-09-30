import React from 'react';
import { AuthProvider } from './context/AuthContext';
import { Navbar } from './components/common/Navbar';
import { ReservasPage } from './pages/ReservasPage';

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <div className="app-layout">
        <Navbar />
        <main className="main-content">
          <ReservasPage />
        </main>
        <footer className="footer">
          <div className="footer-container">
            <p>© 2026 Biblioteca Horizonte - Sistema de Gestión de Reservas para Docentes (MVP)</p>
            <p className="footer-note">Regla fundamental: Una sola reserva confirmada por equipo + fecha + módulo.</p>
          </div>
        </footer>
      </div>
    </AuthProvider>
  );
};

export default App;

