import React, { useState, useEffect, useCallback } from 'react';
import { Reserva, Equipo, CrearReservaDTO } from '../types/reserva';
import { reservaApi } from '../api/reservaApi';
import { equipoApi } from '../api/equipoApi';
import { ApiError } from '../api/apiClient';
import { useAuth } from '../context/AuthContext';
import { LoginForm } from '../components/auth/LoginForm';
import { ReservaForm } from '../components/reserva/ReservaForm';
import { ReservaList } from '../components/reserva/ReservaList';
import { ConsultaConfirmadas } from '../components/reserva/ConsultaConfirmadas';
import { VerificarAccesoSolicitud } from '../components/reserva/VerificarAccesoSolicitud';
import { Alert, AlertType } from '../components/common/Alert';

interface NotificationState {
  type: AlertType;
  title?: string;
  message: string;
  validaciones?: Record<string, string> | null;
}

type TabBibliotecaria = 'solicitudes' | 'confirmadas';

export const ReservasPage: React.FC = () => {
  const { usuario, loading: authLoading } = useAuth();

  const [reservas, setReservas] = useState<Reserva[]>([]);
  const [equipos, setEquipos] = useState<Equipo[]>([]);
  const [loadingReservas, setLoadingReservas] = useState<boolean>(false);
  const [loadingEquipos, setLoadingEquipos] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [processingId, setProcessingId] = useState<number | null>(null);
  const [notification, setNotification] = useState<NotificationState | null>(null);
  const [activeTabBiblio, setActiveTabBiblio] = useState<TabBibliotecaria>('solicitudes');

  // Carga de catálogo de equipos (disponible públicamente)
  const cargarEquipos = useCallback(async () => {
    setLoadingEquipos(true);
    try {
      const data = await equipoApi.listar();
      setEquipos(data);
    } catch {
      setEquipos([]);
    } finally {
      setLoadingEquipos(false);
    }
  }, []);

  // Carga de solicitudes según rol
  // Si es DOCENTE: consume estrictamente /api/v1/reservas/mis-solicitudes (RF09, CP10)
  // Si es BIBLIOTECARIA: consume /api/v1/reservas (todas las solicitudes)
  const cargarReservas = useCallback(async () => {
    if (!usuario) return;

    setLoadingReservas(true);
    try {
      let data: Reserva[];
      if (usuario.rol === 'ROLE_DOCENTE') {
        data = await reservaApi.listarMisSolicitudes();
      } else {
        data = await reservaApi.listar();
      }
      setReservas(data);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 401) {
          setNotification({
            type: 'error',
            title: 'Sesión no válida (401)',
            message: 'La sesión actual no es válida o ha expirado. Por favor, vuelva a identificarse.',
          });
        } else if (err.status === 403) {
          setNotification({
            type: 'error',
            title: 'Acceso Denegado (403)',
            message: err.message || 'No tiene permisos para acceder a esta información.',
          });
        } else {
          setNotification({
            type: 'error',
            title: 'Error al consultar reservas',
            message: err.message,
          });
        }
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
  }, [usuario]);

  useEffect(() => {
    cargarEquipos();
  }, [cargarEquipos]);

  useEffect(() => {
    if (usuario) {
      cargarReservas();
    } else {
      setReservas([]);
      setNotification(null);
    }
  }, [usuario, cargarReservas]);

  /**
   * Manejador para crear una nueva solicitud (DOCENTE).
   * REQUISITO CLAVE: Al registrarse NO debe decir "Reserva realizada",
   * sino indicar explícitamente que quedó en estado PENDIENTE.
   */
  const handleCrearSolicitud = async (dto: CrearReservaDTO): Promise<boolean> => {
    setIsSubmitting(true);
    setNotification(null);
    try {
      const nuevaReserva = await reservaApi.crear(dto);

      setNotification({
        type: 'info',
        title: 'Solicitud Registrada en Estado PENDIENTE',
        message: `La solicitud #${nuevaReserva.id} ha sido registrada con éxito. Su estado actual es PENDIENTE a la espera de confirmación por Biblioteca.`,
      });

      // Refrescar lista de solicitudes propias del docente
      await cargarReservas();
      return true;
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 401) {
          setNotification({
            type: 'error',
            title: 'No Autenticado (401)',
            message: 'Debe iniciar sesión para registrar una solicitud.',
          });
        } else if (err.status === 403) {
          setNotification({
            type: 'error',
            title: 'Permiso Denegado (403)',
            message: err.message,
          });
        } else {
          setNotification({
            type: 'error',
            title: `Error al registrar solicitud (${err.status})`,
            message: err.message,
            validaciones: err.validaciones,
          });
        }
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
   * Manejador para confirmar una solicitud (BIBLIOTECARIA).
   * Controla la regla fundamental RN-001 (backend responde 409 si hay colisión de slot).
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
        if (err.status === 409) {
          setNotification({
            type: 'error',
            title: 'Conflicto de Reserva (RN-001)',
            message: err.message,
          });
        } else if (err.status === 403) {
          setNotification({
            type: 'error',
            title: 'Acceso Denegado (403)',
            message: 'Solo el personal con rol BIBLIOTECARIA puede confirmar solicitudes.',
          });
        } else {
          setNotification({
            type: 'error',
            title: 'Error de Confirmación',
            message: err.message,
          });
        }
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
   * Manejador para rechazar una solicitud (BIBLIOTECARIA).
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
        if (err.status === 403) {
          setNotification({
            type: 'error',
            title: 'Acceso Denegado (403)',
            message: 'Solo el personal con rol BIBLIOTECARIA puede rechazar solicitudes.',
          });
        } else {
          setNotification({
            type: 'error',
            title: 'Error al rechazar solicitud',
            message: err.message,
          });
        }
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

  if (authLoading) {
    return (
      <div className="page-container">
        <div className="empty-state">
          <div className="spinner-large"></div>
          <p>Verificando estado de sesión...</p>
        </div>
      </div>
    );
  }

  // 1. Pantalla de identificación si no hay usuario autenticado
  if (!usuario) {
    return (
      <div className="page-container">
        <LoginForm />
      </div>
    );
  }

  const esDocente = usuario.rol === 'ROLE_DOCENTE';
  const esBibliotecaria = usuario.rol === 'ROLE_BIBLIOTECARIA';

  return (
    <div className="page-container" data-testid="reservas-page">
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

      {/* ========================================================================= */}
      {/* VISTA 1: DOCENTE (RF09, CP10, CP11)                                       */}
      {/* ========================================================================= */}
      {esDocente && (
        <>
          <div className="role-portal-banner portal-docente">
            <div>
              <span className="portal-tag">PORTAL DEL DOCENTE</span>
              <h2 className="portal-title">Bienvenido/a, {usuario.nombreCompleto}</h2>
              <p className="portal-subtitle">
                Gestione sus solicitudes de equipamiento y consulte el estado actualizado de sus reservas.
              </p>
            </div>
            <div className="portal-id-chip">Legajo #{usuario.docenteId || 1}</div>
          </div>

          {/* Formulario de Alta de Solicitud ligado a la identidad activa */}
          <ReservaForm
            equipos={equipos}
            loadingEquipos={loadingEquipos}
            onSubmit={handleCrearSolicitud}
            isSubmitting={isSubmitting}
          />

          {/* Listado Exclusivo de Solicitudes Propias (RF09 / CP10) */}
          <ReservaList
            reservas={reservas}
            loading={loadingReservas}
            onRefresh={cargarReservas}
            processingId={null}
            title="Mis Solicitudes de Reserva (RF09 / CP10)"
            description="Visualice el estado de sus solicitudes. El servidor retorna exclusivamente las reservas pertenecientes a su identidad (/api/v1/reservas/mis-solicitudes)."
            showActions={false}
          />

          {/* Módulo de Verificación de Seguridad y Aislamiento (CP11) */}
          <VerificarAccesoSolicitud />
        </>
      )}

      {/* ========================================================================= */}
      {/* VISTA 2: BIBLIOTECARIA (RF10, CP14, Gestión Administrativa)              */}
      {/* ========================================================================= */}
      {esBibliotecaria && (
        <>
          <div className="role-portal-banner portal-biblio">
            <div>
              <span className="portal-tag tag-biblio">PORTAL DE BIBLIOTECA</span>
              <h2 className="portal-title">Panel de Administración - Lucía</h2>
              <p className="portal-subtitle">
                Gestión centralizada de solicitudes de docentes y control de disponibilidad de recursos escolares.
              </p>
            </div>
          </div>

          {/* Pestañas de Navegación de Biblioteca */}
          <div className="admin-nav-tabs">
            <button
              type="button"
              className={`admin-tab-btn ${activeTabBiblio === 'solicitudes' ? 'tab-active' : ''}`}
              onClick={() => setActiveTabBiblio('solicitudes')}
              data-testid="tab-solicitudes"
            >
              📋 Gestión de Solicitudes Pendientes
            </button>
            <button
              type="button"
              className={`admin-tab-btn ${activeTabBiblio === 'confirmadas' ? 'tab-active' : ''}`}
              onClick={() => setActiveTabBiblio('confirmadas')}
              data-testid="tab-confirmadas"
            >
              📅 Consulta de Reservas Confirmadas (RF10 / CP14)
            </button>
          </div>

          {activeTabBiblio === 'solicitudes' ? (
            <ReservaList
              reservas={reservas}
              loading={loadingReservas}
              onRefresh={cargarReservas}
              onConfirmar={handleConfirmar}
              onRechazar={handleRechazar}
              processingId={processingId}
              title="Gestión de Solicitudes (Todas las Solicitudes)"
              description="Revise solicitudes de todos los docentes, confirme asignaciones o rechace solicitudes según disponibilidad."
              showActions={true}
            />
          ) : (
            <ConsultaConfirmadas
              equipos={equipos}
              loadingEquipos={loadingEquipos}
            />
          )}
        </>
      )}
    </div>
  );
};
