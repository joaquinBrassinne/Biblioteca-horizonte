package com.biblioteca.horizonte.service.impl;

import com.biblioteca.horizonte.dto.request.CrearReservaRequest;
import com.biblioteca.horizonte.dto.response.ReservaResponse;
import com.biblioteca.horizonte.repository.EquipoRepository;
import com.biblioteca.horizonte.repository.ReservaRepository;
import com.biblioteca.horizonte.service.ReservaService;
import org.springframework.stereotype.Service;

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
    public ReservaResponse crearSolicitud(CrearReservaRequest request) {
        throw new UnsupportedOperationException("Lógica de negocio pendiente para Fase 3");
    }

    @Override
    public List<ReservaResponse> listarTodas() {
        return List.of();
    }

    @Override
    public ReservaResponse obtenerPorId(Long id) {
        throw new UnsupportedOperationException("Lógica de negocio pendiente para Fase 3");
    }

    @Override
    public ReservaResponse confirmarSolicitud(Long id) {
        throw new UnsupportedOperationException("Lógica de negocio pendiente para Fase 3");
    }

    @Override
    public ReservaResponse rechazarSolicitud(Long id) {
        throw new UnsupportedOperationException("Lógica de negocio pendiente para Fase 3");
    }
}
