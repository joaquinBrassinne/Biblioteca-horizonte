import React, { useState } from 'react';
import { CrearReservaDTO, Equipo } from '../../types/reserva';

interface ReservaFormProps {
  equipos: Equipo[];
  loadingEquipos: boolean;
  onSubmit: (dto: CrearReservaDTO) => Promise<boolean>;
  isSubmitting: boolean;
}

const MODULOS_DISPONIBLES = [
  { id: 'M1', label: 'Módulo 1 (08:00 - 09:30)' },
  { id: 'M2', label: 'Módulo 2 (09:40 - 11:10)' },
  { id: 'M3', label: 'Módulo 3 (11:20 - 12:50)' },
  { id: 'M4', label: 'Módulo 4 (14:00 - 15:30)' },
  { id: 'M5', label: 'Módulo 5 (15:40 - 17:10)' },
];

export const ReservaForm: React.FC<ReservaFormProps> = ({
  equipos,
  loadingEquipos,
  onSubmit,
  isSubmitting,
}) => {
  const [docenteId, setDocenteId] = useState<string>('1');
  const [equipoId, setEquipoId] = useState<string>('');
  const [fecha, setFecha] = useState<string>('');
  const [modulo, setModulo] = useState<string>('M1');
  const [formError, setFormError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    // Validación básica de formulario requerida
    if (!docenteId || Number(docenteId) <= 0) {
      setFormError('Por favor, ingrese un identificador de docente válido.');
      return;
    }
    if (!equipoId || Number(equipoId) <= 0) {
      setFormError('Debe seleccionar un equipo para la solicitud.');
      return;
    }
    if (!fecha) {
      setFormError('Debe seleccionar una fecha para la reserva.');
      return;
    }
    if (!modulo) {
      setFormError('Debe seleccionar un módulo horario.');
      return;
    }

    const dto: CrearReservaDTO = {
      docenteId: Number(docenteId),
      equipoId: Number(equipoId),
      fecha,
      modulo,
    };

    const exito = await onSubmit(dto);
    if (exito) {
      // Limpiar campos variables tras creación exitosa
      setEquipoId('');
      setFecha('');
      setModulo('M1');
    }
  };

  return (
    <div className="card form-card">
      <div className="card-header">
        <h2 className="card-title">Nueva Solicitud de Reserva</h2>
        <p className="card-description">
          Complete los datos requeridos. La solicitud quedará registrada en estado{' '}
          <strong className="text-amber">PENDIENTE</strong> hasta su posterior confirmación.
        </p>
      </div>

      {formError && (
        <div className="form-error-banner" role="alert">
          {formError}
        </div>
      )}

      <form onSubmit={handleSubmit} className="reserva-form" noValidate>
        <div className="form-grid">
          {/* Docente ID */}
          <div className="form-group">
            <label htmlFor="docenteId" className="form-label">
              ID / Legajo de Docente <span className="required">*</span>
            </label>
            <input
              id="docenteId"
              type="number"
              min="1"
              className="form-input"
              value={docenteId}
              onChange={(e) => setDocenteId(e.target.value)}
              placeholder="Ej: 1"
              required
              disabled={isSubmitting}
            />
            <span className="form-hint">Identificador único del solicitante</span>
          </div>

          {/* Equipo */}
          <div className="form-group">
            <label htmlFor="equipoId" className="form-label">
              Equipo Requerido <span className="required">*</span>
            </label>
            {loadingEquipos ? (
              <div className="form-input-loading">Cargando catálogo de equipos...</div>
            ) : equipos.length > 0 ? (
              <select
                id="equipoId"
                className="form-select"
                value={equipoId}
                onChange={(e) => setEquipoId(e.target.value)}
                required
                disabled={isSubmitting}
              >
                <option value="">-- Seleccione un equipo --</option>
                {equipos.map((eq) => (
                  <option key={eq.id} value={eq.id}>
                    #{eq.id} - {eq.nombre}
                  </option>
                ))}
              </select>
            ) : (
              <input
                id="equipoId"
                type="number"
                min="1"
                className="form-input"
                value={equipoId}
                onChange={(e) => setEquipoId(e.target.value)}
                placeholder="ID numérico del equipo (Ej: 10)"
                required
                disabled={isSubmitting}
              />
            )}
            <span className="form-hint">Recurso físico de biblioteca</span>
          </div>

          {/* Fecha */}
          <div className="form-group">
            <label htmlFor="fecha" className="form-label">
              Fecha de Reserva <span className="required">*</span>
            </label>
            <input
              id="fecha"
              type="date"
              className="form-input"
              value={fecha}
              onChange={(e) => setFecha(e.target.value)}
              required
              disabled={isSubmitting}
            />
            <span className="form-hint">Día en que se utilizará el equipo</span>
          </div>

          {/* Módulo Horario */}
          <div className="form-group">
            <label htmlFor="modulo" className="form-label">
              Módulo Horario <span className="required">*</span>
            </label>
            <select
              id="modulo"
              className="form-select"
              value={modulo}
              onChange={(e) => setModulo(e.target.value)}
              required
              disabled={isSubmitting}
            >
              {MODULOS_DISPONIBLES.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.label}
                </option>
              ))}
            </select>
            <span className="form-hint">Franja horaria académica</span>
          </div>
        </div>

        <div className="form-actions">
          <button
            type="submit"
            className="btn btn-primary"
            disabled={isSubmitting}
            data-testid="submit-reserva-btn"
          >
            {isSubmitting ? (
              <>
                <span className="spinner"></span> Registrando solicitud...
              </>
            ) : (
              'Enviar Solicitud de Reserva'
            )}
          </button>
        </div>
      </form>
    </div>
  );
};
