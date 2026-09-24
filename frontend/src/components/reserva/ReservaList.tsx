import React, { useState } from 'react';
import { Reserva, EstadoReserva } from '../../types/reserva';
import { ReservaCard } from './ReservaCard';

interface ReservaListProps {
  reservas: Reserva[];
  loading: boolean;
  onRefresh: () => void;
  onConfirmar: (id: number) => Promise<void>;
  onRechazar: (id: number) => Promise<void>;
  processingId: number | null;
}

type FilterOption = 'TODAS' | EstadoReserva;

export const ReservaList: React.FC<ReservaListProps> = ({
  reservas,
  loading,
  onRefresh,
  onConfirmar,
  onRechazar,
  processingId,
}) => {
  const [filtro, setFiltro] = useState<FilterOption>('TODAS');

  const countPendientes = reservas.filter((r) => r.estado === 'PENDIENTE').length;
  const countConfirmadas = reservas.filter((r) => r.estado === 'CONFIRMADA').length;
  const countRechazadas = reservas.filter((r) => r.estado === 'RECHAZADA').length;

  const reservasFiltradas = reservas.filter((r) => {
    if (filtro === 'TODAS') return true;
    return r.estado === filtro;
  });

  return (
    <div className="card list-card">
      <div className="card-header list-header">
        <div>
          <h2 className="card-title">Listado de Solicitudes</h2>
          <p className="card-description">
            Visualice el estado en tiempo real y gestione la confirmación o rechazo.
          </p>
        </div>
        <button
          type="button"
          className="btn btn-outline"
          onClick={onRefresh}
          disabled={loading}
          title="Actualizar lista desde el servidor"
          data-testid="refresh-btn"
        >
          {loading ? 'Actualizando...' : '↻ Actualizar'}
        </button>
      </div>

      {/* Filtros rápidos con contadores */}
      <div className="filter-bar">
        <button
          type="button"
          className={`filter-btn ${filtro === 'TODAS' ? 'filter-active' : ''}`}
          onClick={() => setFiltro('TODAS')}
        >
          Todas <span className="counter-pill">{reservas.length}</span>
        </button>
        <button
          type="button"
          className={`filter-btn filter-btn-pendiente ${filtro === 'PENDIENTE' ? 'filter-active' : ''}`}
          onClick={() => setFiltro('PENDIENTE')}
        >
          Pendientes <span className="counter-pill pill-amber">{countPendientes}</span>
        </button>
        <button
          type="button"
          className={`filter-btn filter-btn-confirmada ${filtro === 'CONFIRMADA' ? 'filter-active' : ''}`}
          onClick={() => setFiltro('CONFIRMADA')}
        >
          Confirmadas <span className="counter-pill pill-green">{countConfirmadas}</span>
        </button>
        <button
          type="button"
          className={`filter-btn filter-btn-rechazada ${filtro === 'RECHAZADA' ? 'filter-active' : ''}`}
          onClick={() => setFiltro('RECHAZADA')}
        >
          Rechazadas <span className="counter-pill pill-red">{countRechazadas}</span>
        </button>
      </div>

      {/* Contenido principal */}
      {loading && reservas.length === 0 ? (
        <div className="empty-state">
          <div className="spinner-large"></div>
          <p>Cargando solicitudes de reserva...</p>
        </div>
      ) : reservasFiltradas.length === 0 ? (
        <div className="empty-state">
          <svg className="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
            <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
          </svg>
          <h3>No hay solicitudes para mostrar</h3>
          <p>
            {filtro === 'TODAS'
              ? 'Aún no se ha registrado ninguna solicitud de reserva. Utilice el formulario superior para enviar una nueva solicitud.'
              : `No existen solicitudes en estado ${filtro}.`}
          </p>
        </div>
      ) : (
        <div className="reservas-grid">
          {reservasFiltradas.map((reserva) => (
            <ReservaCard
              key={reserva.id}
              reserva={reserva}
              onConfirmar={onConfirmar}
              onRechazar={onRechazar}
              isProcessing={processingId === reserva.id}
            />
          ))}
        </div>
      )}
    </div>
  );
};
