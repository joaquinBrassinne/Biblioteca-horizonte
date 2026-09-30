import React, { useState } from 'react';
import { Reserva } from '../../types/reserva';
import { reservaApi } from '../../api/reservaApi';
import { ApiError } from '../../api/apiClient';
import { Alert } from '../common/Alert';
import { StatusBadge } from '../common/StatusBadge';

export const VerificarAccesoSolicitud: React.FC = () => {
  const [solicitudId, setSolicitudId] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [reservaEncontrada, setReservaEncontrada] = useState<Reserva | null>(null);
  const [alerta, setAlerta] = useState<{
    type: 'error' | 'success' | 'warning';
    title: string;
    message: string;
  } | null>(null);

  const handleConsultar = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!solicitudId || Number(solicitudId) <= 0) {
      setAlerta({
        type: 'warning',
        title: 'ID Inválido',
        message: 'Por favor ingrese un número de solicitud válido.',
      });
      return;
    }

    setLoading(true);
    setAlerta(null);
    setReservaEncontrada(null);

    try {
      // Consume el endpoint /api/v1/reservas/{id} directamente
      const data = await reservaApi.obtenerPorId(Number(solicitudId));
      setReservaEncontrada(data);
      setAlerta({
        type: 'success',
        title: 'Acceso Permitido (200 OK)',
        message: `Solicitud #${data.id} consultada exitosamente. Pertenece a su legajo.`,
      });
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 403) {
          // CP11: El backend rechaza la consulta de una solicitud ajena
          setAlerta({
            type: 'error',
            title: '403 Acceso Denegado (Verificación CP11)',
            message: `El backend rechazó el acceso: ${err.message || 'No tiene permisos para consultar solicitudes de otro docente.'}`,
          });
        } else if (err.status === 404) {
          setAlerta({
            type: 'warning',
            title: '404 No Encontrada',
            message: `No existe ninguna solicitud registrada con ID #${solicitudId}.`,
          });
        } else {
          setAlerta({
            type: 'error',
            title: `Error (${err.status})`,
            message: err.message,
          });
        }
      } else {
        setAlerta({
          type: 'error',
          title: 'Error de Red',
          message: 'No fue posible comunicarse con el servidor.',
        });
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card cp11-verification-card" data-testid="cp11-verification-card">
      <div className="card-header">
        <h3 className="card-title">Verificación de Aislamiento por Identidad (CP11)</h3>
        <p className="card-description">
          Pruebe acceder directamente a cualquier solicitud por identificador. El servidor validará si la solicitud le
          pertenece o responderá con <strong>403 Forbidden</strong> si pertenece a otro docente.
        </p>
      </div>

      {alerta && (
        <Alert
          type={alerta.type}
          title={alerta.title}
          message={alerta.message}
          onClose={() => setAlerta(null)}
        />
      )}

      <form onSubmit={handleConsultar} className="cp11-form" noValidate>
        <div className="cp11-input-group">
          <label htmlFor="cp11-solicitud-id" className="form-label visually-hidden">
            ID de Solicitud
          </label>
          <input
            id="cp11-solicitud-id"
            type="number"
            min="1"
            className="form-input"
            value={solicitudId}
            onChange={(e) => setSolicitudId(e.target.value)}
            placeholder="Ingrese ID de solicitud (ej: 1, 2, 5...)"
            disabled={loading}
            data-testid="cp11-input-id"
          />
          <button
            type="submit"
            className="btn btn-outline"
            disabled={loading}
            data-testid="cp11-submit-btn"
          >
            {loading ? 'Consultando...' : '🔍 Probar Acceso por ID'}
          </button>
        </div>
      </form>

      {reservaEncontrada && (
        <div className="cp11-detalle-box">
          <div className="reserva-card card-estado-confirmada">
            <div className="reserva-card-top">
              <span className="reserva-id-badge">Solicitud #{reservaEncontrada.id}</span>
              <StatusBadge estado={reservaEncontrada.estado} />
            </div>
            <div className="reserva-card-details">
              <div className="detail-item">
                <span className="detail-label">Equipo:</span>
                <span className="detail-value">{reservaEncontrada.equipoNombre || reservaEncontrada.equipoId}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Docente:</span>
                <span className="detail-value">Legajo {reservaEncontrada.docenteId}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Fecha:</span>
                <span className="detail-value">{reservaEncontrada.fecha}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Módulo:</span>
                <span className="detail-value">{reservaEncontrada.modulo}</span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
