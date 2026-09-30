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
import com.biblioteca.horizonte.security.DocenteResolver;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.biblioteca.horizonte.service.AuditoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;
    private final EquipoRepository equipoRepository;
    private final DocenteResolver docenteResolver;
    private final AuditoriaService auditoriaService;

    public ReservaServiceImpl(ReservaRepository reservaRepository, EquipoRepository equipoRepository) {
        this(reservaRepository, equipoRepository, new DocenteResolver(), null);
    }

    public ReservaServiceImpl(ReservaRepository reservaRepository,
                              EquipoRepository equipoRepository,
                              DocenteResolver docenteResolver) {
        this(reservaRepository, equipoRepository, docenteResolver, null);
    }

    @Autowired
    public ReservaServiceImpl(ReservaRepository reservaRepository,
                              EquipoRepository equipoRepository,
                              DocenteResolver docenteResolver,
                              AuditoriaService auditoriaService) {
        this.reservaRepository = reservaRepository;
        this.equipoRepository = equipoRepository;
        this.docenteResolver = docenteResolver != null ? docenteResolver : new DocenteResolver();
        this.auditoriaService = auditoriaService;
    }


    @Override
    @Transactional
    public ReservaResponse crearSolicitud(CrearReservaRequest request) {
        Equipo equipo = equipoRepository.findById(request.getEquipoId())
                .orElseThrow(() -> new ResourceNotFoundException("El equipo con ID " + request.getEquipoId() + " no existe."));

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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            boolean esDocente = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_DOCENTE"));
            boolean esBibliotecaria = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_BIBLIOTECARIA"));

            if (esDocente && !esBibliotecaria) {
                Long docenteId = docenteResolver.resolverDocenteId(auth.getName());
                return listarMisSolicitudes(docenteId);
            }
        }

        return reservaRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResponse> listarMisSolicitudes(Long docenteId) {
        return reservaRepository.findByDocenteId(docenteId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResponse> listarConfirmadas(Long equipoId, LocalDate fecha) {
        List<Reserva> reservas;
        if (equipoId != null && fecha != null) {
            reservas = reservaRepository.findByEquipoIdAndFechaAndEstado(equipoId, fecha, EstadoReserva.CONFIRMADA);
        } else if (fecha != null) {
            reservas = reservaRepository.findByFechaAndEstado(fecha, EstadoReserva.CONFIRMADA);
        } else if (equipoId != null) {
            reservas = reservaRepository.findByEquipoIdAndEstado(equipoId, EstadoReserva.CONFIRMADA);
        } else {
            reservas = reservaRepository.findByEstado(EstadoReserva.CONFIRMADA);
        }

        return reservas.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaResponse obtenerPorId(Long id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reserva con ID: " + id));

        verificarAccesoDocente(reserva);

        return mapToResponse(reserva);
    }

    private void verificarAccesoDocente(Reserva reserva) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            boolean esDocente = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_DOCENTE"));
            boolean esBibliotecaria = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_BIBLIOTECARIA"));

            if (esDocente && !esBibliotecaria) {
                Long docenteAutenticadoId = docenteResolver.resolverDocenteId(auth.getName());
                if (!reserva.getDocenteId().equals(docenteAutenticadoId)) {
                    throw new AccessDeniedException("No tiene permisos para consultar solicitudes de otro docente.");
                }
            }
        }
    }

    @Override
    @Transactional
    public ReservaResponse confirmarSolicitud(Long id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reserva con ID: " + id));

        // Regla RN-003: Solo se puede confirmar una solicitud en estado PENDIENTE
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new InvalidStateTransitionException(
                    "Solo se pueden confirmar solicitudes en estado PENDIENTE. Estado actual: " + reserva.getEstado() + "."
            );
        }

        // Regla RN-001: No puede existir más de una reserva CONFIRMADA para el mismo equipo + fecha + módulo
        boolean ocupado = reservaRepository.existsByEquipoIdAndFechaAndModuloAndEstado(
                reserva.getEquipo().getId(),
                reserva.getFecha(),
                reserva.getModulo(),
                EstadoReserva.CONFIRMADA
        );

        if (ocupado) {
            throw new ReservaConflictException(
                    "No es posible confirmar la reserva. Ya existe otra reserva confirmada para el equipo "
                            + reserva.getEquipo().getId() + " en la fecha " + reserva.getFecha() + " y módulo " + reserva.getModulo() + "."
            );
        }

        reserva.setEstado(EstadoReserva.CONFIRMADA);
        Reserva actualizada = reservaRepository.save(reserva);

        if (auditoriaService != null) {
            auditoriaService.registrarAuditoria(actualizada, "CONFIRMAR", "EXITOSO");
        }

        return mapToResponse(actualizada);
    }

    @Override
    @Transactional
    public ReservaResponse rechazarSolicitud(Long id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reserva con ID: " + id));

        // Regla RN-003: Solo se puede rechazar una solicitud en estado PENDIENTE
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new InvalidStateTransitionException(
                    "Solo se pueden rechazar solicitudes en estado PENDIENTE. Estado actual: " + reserva.getEstado() + "."
            );
        }

        reserva.setEstado(EstadoReserva.RECHAZADA);
        Reserva actualizada = reservaRepository.save(reserva);

        if (auditoriaService != null) {
            auditoriaService.registrarAuditoria(actualizada, "RECHAZAR", "EXITOSO");
        }

        return mapToResponse(actualizada);
    }


    private ReservaResponse mapToResponse(Reserva reserva) {
        return new ReservaResponse(
                reserva.getId(),
                reserva.getDocenteId(),
                reserva.getEquipo().getId(),
                reserva.getEquipo().getNombre(),
                reserva.getFecha(),
                reserva.getModulo(),
                reserva.getEstado(),
                reserva.getFechaCreacion()
        );
    }
}
