import React, { useState, useEffect, useCallback } from 'react';
import { Reserva, Equipo, CrearReservaDTO } from '../types/reserva';
import { reservaApi } from '../api/reservaApi';
import { equipoApi } from '../api/equipoApi';
import { ApiError } from '../api/apiClient';
import { ReservaForm } from '../components/reserva/ReservaForm';
import { ReservaList } from '../components/reserva/ReservaList';
import { Alert, AlertType } from '../components/common/Alert';

interface NotificationState {
  type: AlertType;
  title?: string;
  message: string;
  validaciones?: Record<string, string> | null;
}

export const ReservasPage: React.FC = () => {
  const [reservas, setReservas] = useState<Reserva[]>([]);
  const [equipos, setEquipos] = useState<Equipo[]>([]);
  const [loadingReservas, setLoadingReservas] = useState<boolean>(false);
  const [loadingEquipos, setLoadingEquipos] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [processingId, setProcessingId] = useState<number | null>(null);
  const [notification, setNotification] = useState<NotificationState | null>(null);

  // Carga de catálogo de equipos
  const cargarEquipos = useCallback(async () => {
    setLoadingEquipos(true);
    try {
      const data = await equipoApi.listar();
      setEquipos(data);
    } catch {
      // Si el endpoint de equipos aún no tiene datos o falla, no bloquea el flujo manual
      setEquipos([]);
    } finally {
      setLoadingEquipos(false);
    }
  }, []);

  // Carga de solicitudes de reservas
  const cargarReservas = useCallback(async () => {
    setLoadingReservas(true);
    try {
      const data = await reservaApi.listar();
      setReservas(data);
    } catch (err) {
      if (err instanceof ApiError) {
        setNotification({
          type: 'error',
          title: 'Error al consultar reservas',
          message: err.message,
        });
      } else {
        setNotification({
          type: 'error',
          title: 'Error de conexión',
          message: 'No fue posible comunicarse con el backend.',
        });
      }
    } finally {
      setLoadingReservas(false);
    }
  }, []);

  useEffect(() => {
    cargarEquipos();
    cargarReservas();
  }, [cargarEquipos, cargarReservas]);

  /**
   * Manejador para crear una nueva solicitud.
   * REQUISITO CLAVE: Al registrarse NO debe decir "Reserva realizada",
   * sino indicar explícitamente que quedó en estado PENDIENTE.
   */
  const handleCrearSolicitud = async (dto: CrearReservaDTO): Promise<boolean> => {
    setIsSubmitting(true);
    setNotification(null);
    try {
      const nuevaReserva = await reservaApi.crear(dto);
      
      // REQUISITO ESTRICTO: NO MOSTRAR "Reserva realizada". Mostrar "Pendiente".
      setNotification({
        type: 'info',
        title: 'Solicitud Registrada en Estado PENDIENTE',
        message: `La solicitud #${nuevaReserva.id} ha sido registrada con éxito. Su estado actual es PENDIENTE a la espera de confirmación.`,
      });

      // Refrescar lista de reservas
      await cargarReservas();
      return true;
    } catch (err) {
      if (err instanceof ApiError) {
        setNotification({
          type: 'error',
          title: `Error al registrar solicitud (${err.status})`,
          message: err.message,
          validaciones: err.validaciones,
        });
      } else {
        setNotification({
          type: 'error',
          title: 'Error inesperado',
          message: 'Ocurrió un error al enviar la solicitud.',
        });
      }
      return false;
    } finally {
      setIsSubmitting(false);
    }
  };

  /**
   * Manejador para confirmar una solicitud.
   * Controla la regla fundamental RN-001 (backend responde 409 si hay conflicto).
   */
  const handleConfirmar = async (id: number): Promise<void> => {
    setProcessingId(id);
    setNotification(null);
    try {
      const reservaConfirmada = await reservaApi.confirmar(id);
      setNotification({
        type: 'success',
        title: 'Solicitud CONFIRMADA',
        message: `La solicitud #${reservaConfirmada.id} fue confirmada exitosamente. El equipo quedó asignado para la fecha y módulo seleccionados.`,
      });
      await cargarReservas();
    } catch (err) {
      if (err instanceof ApiError) {
        // En caso de conflicto de slot (RN-001) o estado inválido
        setNotification({
          type: 'error',
          title: err.status === 409 ? 'Conflicto de Reserva (RN-001)' : 'Error de Confirmación',
          message: err.message,
        });
      } else {
        setNotification({
          type: 'error',
          title: 'Error de comunicación',
          message: 'No se pudo completar la confirmación.',
        });
      }
    } finally {
      setProcessingId(null);
    }
  };

  /**
   * Manejador para rechazar una solicitud.
   */
  const handleRechazar = async (id: number): Promise<void> => {
    setProcessingId(id);
    setNotification(null);
    try {
      const reservaRechazada = await reservaApi.rechazar(id);
      setNotification({
        type: 'warning',
        title: 'Solicitud RECHAZADA',
        message: `La solicitud #${reservaRechazada.id} ha sido rechazada. El recurso permanece disponible para otras solicitudes.`,
      });
      await cargarReservas();
    } catch (err) {
      if (err instanceof ApiError) {
        setNotification({
          type: 'error',
          title: 'Error al rechazar solicitud',
          message: err.message,
        });
      } else {
        setNotification({
          type: 'error',
          title: 'Error de comunicación',
          message: 'No se pudo completar el rechazo.',
        });
      }
    } finally {
      setProcessingId(null);
    }
  };

  return (
    <div className="page-container">
      {/* Alertas y Notificaciones Globales */}
      {notification && (
        <Alert
          type={notification.type}
          title={notification.title}
          message={notification.message}
          validaciones={notification.validaciones}
          onClose={() => setNotification(null)}
        />
      )}

      {/* Formulario de Alta de Solicitud */}
      <ReservaForm
        equipos={equipos}
        loadingEquipos={loadingEquipos}
        onSubmit={handleCrearSolicitud}
        isSubmitting={isSubmitting}
      />

      {/* Grilla / Listado de Solicitudes Existentes */}
      <ReservaList
        reservas={reservas}
        loading={loadingReservas}
        onRefresh={cargarReservas}
        onConfirmar={handleConfirmar}
        onRechazar={handleRechazar}
        processingId={processingId}
      />
    </div>
  );
};
