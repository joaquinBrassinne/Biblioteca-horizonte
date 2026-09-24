import React from 'react';
import { Reserva } from '../../types/reserva';
import { StatusBadge } from '../common/StatusBadge';

interface ReservaCardProps {
  reserva: Reserva;
  onConfirmar: (id: number) => Promise<void>;
  onRechazar: (id: number) => Promise<void>;
  isProcessing: boolean;
}

export const ReservaCard: React.FC<ReservaCardProps> = ({
  reserva,
  onConfirmar,
  onRechazar,
  isProcessing,
}) => {
  const isPendiente = reserva.estado === 'PENDIENTE';
  const isConfirmada = reserva.estado === 'CONFIRMADA';
  const isRechazada = reserva.estado === 'RECHAZADA';

  return (
    <div
      className={`reserva-card card-estado-${reserva.estado.toLowerCase()}`}
      data-testid={`reserva-card-${reserva.id}`}
    >
      <div className="reserva-card-top">
        <div className="reserva-id-badge">Solicitud #{reserva.id}</div>
        <StatusBadge estado={reserva.estado} />
      </div>

      <div className="reserva-card-details">
        <div className="detail-item">
          <span className="detail-label">Equipo</span>
          <span className="detail-value">
            {reserva.equipoNombre || `Equipo ID: ${reserva.equipoId}`}
          </span>
        </div>

        <div className="detail-item">
          <span className="detail-label">Docente</span>
          <span className="detail-value">Legajo/ID: {reserva.docenteId}</span>
        </div>

        <div className="detail-item">
          <span className="detail-label">Fecha</span>
          <span className="detail-value detail-date">{reserva.fecha}</span>
        </div>

        <div className="detail-item">
          <span className="detail-label">Módulo</span>
          <span className="detail-value detail-modulo">{reserva.modulo}</span>
        </div>
      </div>

      <div className="reserva-card-footer">
        <div className="reserva-timestamp">
          Registrada: {new Date(reserva.fechaCreacion).toLocaleString()}
        </div>

        {isPendiente && (
          <div className="reserva-actions">
            <button
              type="button"
              className="btn btn-sm btn-confirmar"
              onClick={() => onConfirmar(reserva.id)}
              disabled={isProcessing}
              title="Aprobar y pasar a estado CONFIRMADA"
              data-testid={`btn-confirmar-${reserva.id}`}
            >
              {isProcessing ? 'Procesando...' : 'Confirmar'}
            </button>
            <button
              type="button"
              className="btn btn-sm btn-rechazar"
              onClick={() => onRechazar(reserva.id)}
              disabled={isProcessing}
              title="Desestimar y pasar a estado RECHAZADA"
              data-testid={`btn-rechazar-${reserva.id}`}
            >
              {isProcessing ? 'Procesando...' : 'Rechazar'}
            </button>
          </div>
        )}

        {isConfirmada && (
          <div className="state-notice notice-confirmada">
            <span>Reserva confirmada. Slot asegurado.</span>
          </div>
        )}

        {isRechazada && (
          <div className="state-notice notice-rechazada">
            <span>Solicitud rechazada. Slot liberado.</span>
          </div>
        )}
      </div>
    </div>
  );
};
