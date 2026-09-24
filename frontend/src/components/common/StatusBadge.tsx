import React from 'react';
import { EstadoReserva } from '../../types/reserva';

interface StatusBadgeProps {
  estado: EstadoReserva;
  size?: 'sm' | 'md' | 'lg';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ estado, size = 'md' }) => {
  const getBadgeConfig = () => {
    switch (estado) {
      case 'PENDIENTE':
        return {
          label: 'PENDIENTE',
          sublabel: 'En espera de confirmación',
          className: 'badge-pendiente',
          icon: (
            <svg className="badge-icon" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clipRule="evenodd" />
            </svg>
          ),
        };
      case 'CONFIRMADA':
        return {
          label: 'CONFIRMADA',
          sublabel: 'Asignación asegurada',
          className: 'badge-confirmada',
          icon: (
            <svg className="badge-icon" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
            </svg>
          ),
        };
      case 'RECHAZADA':
        return {
          label: 'RECHAZADA',
          sublabel: 'Solicitud desestimada',
          className: 'badge-rechazada',
          icon: (
            <svg className="badge-icon" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
            </svg>
          ),
        };
    }
  };

  const config = getBadgeConfig();

  return (
    <span
      className={`status-badge ${config.className} badge-${size}`}
      title={config.sublabel}
      data-testid={`badge-${estado.toLowerCase()}`}
    >
      {config.icon}
      <span className="badge-text">{config.label}</span>
    </span>
  );
};
