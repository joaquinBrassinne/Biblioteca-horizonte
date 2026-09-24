package com.biblioteca.horizonte.service.impl;

import com.biblioteca.horizonte.dto.request.CrearReservaRequest;
import com.biblioteca.horizonte.dto.response.ReservaResponse;
import com.biblioteca.horizonte.entity.Equipo;
import com.biblioteca.horizonte.entity.EstadoReserva;
import com.biblioteca.horizonte.entity.Reserva;
import com.biblioteca.horizonte.exception.InvalidStateTransitionException;
import com.biblioteca.horizonte.exception.ReservaConflictException;
import com.biblioteca.horizonte.exception.ResourceNotFoundException;
import com.biblioteca.horizonte.repository.EquipoRepository;
import com.biblioteca.horizonte.repository.ReservaRepository;
import com.biblioteca.horizonte.service.ReservaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;
    private final EquipoRepository equipoRepository;

    public ReservaServiceImpl(ReservaRepository reservaRepository, EquipoRepository equipoRepository) {
        this.reservaRepository = reservaRepository;
        this.equipoRepository = equipoRepository;
    }

    @Override
    @Transactional
    public ReservaResponse crearSolicitud(CrearReservaRequest request) {
        Equipo equipo = equipoRepository.findById(request.getEquipoId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el equipo con ID " + request.getEquipoId()));

        // Regla RN-002: Estado inicial siempre PENDIENTE
        Reserva reserva = new Reserva();
        reserva.setDocenteId(request.getDocenteId());
        reserva.setEquipo(equipo);
        reserva.setFecha(request.getFecha());
        reserva.setModulo(request.getModulo());
        reserva.setEstado(EstadoReserva.PENDIENTE);
        reserva.setFechaCreacion(LocalDateTime.now());

        Reserva guardada = reservaRepository.save(reserva);
        return mapToResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResponse> listarTodas() {
        return reservaRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaResponse obtenerPorId(Long id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reserva con ID " + id));
        return mapToResponse(reserva);
    }

    @Override
    @Transactional
    public ReservaResponse confirmarSolicitud(Long id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reserva con ID " + id));

        // Regla RN-003: Solo se puede confirmar si está en estado PENDIENTE
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new InvalidStateTransitionException(
                    "Solo se pueden confirmar solicitudes en estado PENDIENTE. Estado actual: " + reserva.getEstado() + "."
            );
        }

        // Regla RN-001: No puede existir más de una reserva CONFIRMADA para el mismo equipo + fecha + módulo
        boolean existeConfirmada = reservaRepository.existsByEquipoIdAndFechaAndModuloAndEstado(
                reserva.getEquipo().getId(),
                reserva.getFecha(),
                reserva.getModulo(),
                EstadoReserva.CONFIRMADA
        );

        if (existeConfirmada) {
            throw new ReservaConflictException(
                    "No es posible confirmar la reserva. Ya existe otra reserva confirmada para el equipo " +
                            reserva.getEquipo().getId() + " en la fecha " + reserva.getFecha() + " y módulo " + reserva.getModulo() + "."
            );
        }

        reserva.setEstado(EstadoReserva.CONFIRMADA);
        Reserva actualizada = reservaRepository.save(reserva);
        return mapToResponse(actualizada);
    }

    @Override
    @Transactional
    public ReservaResponse rechazarSolicitud(Long id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reserva con ID " + id));

        // Regla RN-003: Solo se puede rechazar si está en estado PENDIENTE
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new InvalidStateTransitionException(
                    "Solo se pueden rechazar solicitudes en estado PENDIENTE. Estado actual: " + reserva.getEstado() + "."
            );
        }

        reserva.setEstado(EstadoReserva.RECHAZADA);
        Reserva actualizada = reservaRepository.save(reserva);
        return mapToResponse(actualizada);
    }

    private ReservaResponse mapToResponse(Reserva reserva) {
        return new ReservaResponse(
                reserva.getId(),
                reserva.getDocenteId(),
                reserva.getEquipo() != null ? reserva.getEquipo().getId() : null,
                reserva.getEquipo() != null ? reserva.getEquipo().getNombre() : null,
                reserva.getFecha(),
                reserva.getModulo(),
                reserva.getEstado(),
                reserva.getFechaCreacion()
        );
    }
}
