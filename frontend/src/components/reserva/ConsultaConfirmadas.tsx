import React, { useState } from 'react';
import { Reserva, Equipo } from '../../types/reserva';
import { reservaApi } from '../../api/reservaApi';
import { ApiError } from '../../api/apiClient';
import { StatusBadge } from '../common/StatusBadge';
import { Alert } from '../common/Alert';

interface ConsultaConfirmadasProps {
  equipos: Equipo[];
  loadingEquipos: boolean;
}

export const ConsultaConfirmadas: React.FC<ConsultaConfirmadasProps> = ({
  equipos,
  loadingEquipos,
}) => {
  const [equipoId, setEquipoId] = useState<string>('');
  const [fecha, setFecha] = useState<string>('');
  const [reservasConfirmadas, setReservasConfirmadas] = useState<Reserva[]>([]);
  const [haBuscado, setHaBuscado] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const handleBuscar = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    setErrorMsg(null);
    setLoading(true);

    try {
      const data = await reservaApi.listarConfirmadas({
        equipoId: equipoId ? Number(equipoId) : undefined,
        fecha: fecha || undefined,
      });
      setReservasConfirmadas(data);
      setHaBuscado(true);
    } catch (err) {
      if (err instanceof ApiError) {
        setErrorMsg(err.message);
      } else {
        setErrorMsg('Error al consultar reservas confirmadas en el servidor.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleLimpiar = () => {
    setEquipoId('');
    setFecha('');
    setReservasConfirmadas([]);
    setHaBuscado(false);
    setErrorMsg(null);
  };

  return (
    <div className="card consulta-confirmadas-card" data-testid="consulta-confirmadas-section">
      <div className="card-header">
        <div>
          <h2 className="card-title">Consulta de Reservas Confirmadas (RF10 / CP14)</h2>
          <p className="card-description">
            Consulte la asignación efectiva de recursos y slots horarios garantizados. Consume el endpoint del servidor{' '}
            <code>/api/v1/reservas/confirmadas</code>.
          </p>
        </div>
      </div>

      {errorMsg && (
        <Alert
          type="error"
          title="Error en la Consulta"
          message={errorMsg}
          onClose={() => setErrorMsg(null)}
        />
      )}

      {/* Formulario de Filtros en Servidor */}
      <form onSubmit={handleBuscar} className="filtros-form" noValidate>
        <div className="form-grid">
          <div className="form-group">
            <label htmlFor="filtro-equipo" className="form-label">
              Recurso / Equipo
            </label>
            {loadingEquipos ? (
              <div className="form-input-loading">Cargando equipos...</div>
            ) : (
              <select
                id="filtro-equipo"
                className="form-select"
                value={equipoId}
                onChange={(e) => setEquipoId(e.target.value)}
                disabled={loading}
                data-testid="filtro-equipo-select"
              >
                <option value="">-- Todos los recursos --</option>
                {equipos.map((eq) => (
                  <option key={eq.id} value={eq.id}>
                    #{eq.id} - {eq.nombre}
                  </option>
                ))}
              </select>
            )}
            <span className="form-hint">Filtro opcional por equipo físico</span>
          </div>

          <div className="form-group">
            <label htmlFor="filtro-fecha" className="form-label">
              Fecha de Reserva
            </label>
            <input
              id="filtro-fecha"
              type="date"
              className="form-input"
              value={fecha}
              onChange={(e) => setFecha(e.target.value)}
              disabled={loading}
              data-testid="filtro-fecha-input"
            />
            <span className="form-hint">Filtro opcional por fecha exacta</span>
          </div>
        </div>

        <div className="form-actions">
          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading}
            data-testid="btn-buscar-confirmadas"
          >
            {loading ? (
              <>
                <span className="spinner"></span> Consultando...
              </>
            ) : (
              '🔍 Buscar Reservas Confirmadas'
            )}
          </button>

          {(equipoId || fecha || haBuscado) && (
            <button
              type="button"
              className="btn btn-outline"
              onClick={handleLimpiar}
              disabled={loading}
              data-testid="btn-limpiar-filtros"
            >
              Limpiar Filtros
            </button>
          )}
        </div>
      </form>

      {/* Resultados de la Consulta */}
      <div className="consulta-resultados">
        {loading ? (
          <div className="empty-state">
            <div className="spinner-large"></div>
            <p>Consultando reservas confirmadas en el servidor...</p>
          </div>
        ) : haBuscado ? (
          reservasConfirmadas.length === 0 ? (
            <div className="empty-state empty-confirmadas" data-testid="mensaje-no-confirmadas">
              <svg className="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
              </svg>
              <h3>Sin reservas confirmadas</h3>
              <p className="no-results-text">
                No hay reservas confirmadas para los filtros seleccionados.
              </p>
              <span className="empty-subtext">
                Las solicitudes en estado PENDIENTE o RECHAZADA no se consideran reservas asignadas.
              </span>
            </div>
          ) : (
            <div className="confirmadas-results-box" data-testid="resultados-confirmadas">
              <div className="results-summary">
                <span>
                  Se encontraron <strong>{reservasConfirmadas.length}</strong> reserva(s) confirmada(s)
                </span>
                <span className="badge-server-note">Filtradas por servidor</span>
              </div>

              <div className="reservas-grid">
                {reservasConfirmadas.map((r) => (
                  <div
                    key={r.id}
                    className="reserva-card card-estado-confirmada"
                    data-testid={`confirmada-card-${r.id}`}
                  >
                    <div className="reserva-card-top">
                      <div className="reserva-id-badge">Reserva #{r.id}</div>
                      <StatusBadge estado={r.estado} />
                    </div>

                    <div className="reserva-card-details">
                      <div className="detail-item">
                        <span className="detail-label">Recurso</span>
                        <span className="detail-value">{r.equipoNombre || `Equipo ID: ${r.equipoId}`}</span>
                      </div>
                      <div className="detail-item">
                        <span className="detail-label">Docente</span>
                        <span className="detail-value">Legajo: {r.docenteId}</span>
                      </div>
                      <div className="detail-item">
                        <span className="detail-label">Fecha</span>
                        <span className="detail-value detail-date">{r.fecha}</span>
                      </div>
                      <div className="detail-item">
                        <span className="detail-label">Módulo</span>
                        <span className="detail-value detail-modulo">{r.modulo}</span>
                      </div>
                    </div>

                    <div className="reserva-card-footer">
                      <div className="state-notice notice-confirmada">
                        <span>Asignación asegurada y confirmada</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )
        ) : (
          <div className="consulta-hint">
            <p>Seleccione un equipo, una fecha o ambos filtros y haga clic en "Buscar Reservas Confirmadas".</p>
          </div>
        )}
      </div>
    </div>
  );
};
