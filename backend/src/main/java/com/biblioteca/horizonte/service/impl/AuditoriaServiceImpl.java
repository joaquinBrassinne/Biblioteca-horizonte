package com.biblioteca.horizonte.service.impl;

import com.biblioteca.horizonte.dto.response.AuditoriaResponse;
import com.biblioteca.horizonte.entity.AuditoriaReserva;
import com.biblioteca.horizonte.entity.Reserva;
import com.biblioteca.horizonte.repository.AuditoriaReservaRepository;
import com.biblioteca.horizonte.service.AuditoriaService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditoriaServiceImpl implements AuditoriaService {

    private final AuditoriaReservaRepository auditoriaReservaRepository;

    public AuditoriaServiceImpl(AuditoriaReservaRepository auditoriaReservaRepository) {
        this.auditoriaReservaRepository = auditoriaReservaRepository;
    }

    @Override
    @Transactional
    public void registrarAuditoria(Reserva reserva, String operacion, String resultado) {
        String usuario = "SISTEMA";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null && !auth.getName().isBlank()) {
            usuario = auth.getName();
        }

        AuditoriaReserva audit = new AuditoriaReserva(
                reserva,
                usuario,
                operacion,
                LocalDateTime.now(),
                resultado
        );

        auditoriaReservaRepository.save(audit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditoriaResponse> obtenerPorReservaId(Long reservaId) {
        return auditoriaReservaRepository.findByReservaId(reservaId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditoriaResponse> listarTodas() {
        return auditoriaReservaRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AuditoriaResponse mapToResponse(AuditoriaReserva audit) {
        return new AuditoriaResponse(
                audit.getId(),
                audit.getReserva() != null ? audit.getReserva().getId() : null,
                audit.getUsuario(),
                audit.getOperacion(),
                audit.getFechaHora(),
                audit.getResultado()
        );
    }
}
