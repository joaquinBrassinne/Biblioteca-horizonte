package com.biblioteca.horizonte.service;

import com.biblioteca.horizonte.dto.request.CrearReservaRequest;
import com.biblioteca.horizonte.dto.response.ReservaResponse;

import java.util.List;

public interface ReservaService {

    ReservaResponse crearSolicitud(CrearReservaRequest request);

    List<ReservaResponse> listarTodas();

    ReservaResponse obtenerPorId(Long id);

    ReservaResponse confirmarSolicitud(Long id);

    ReservaResponse rechazarSolicitud(Long id);
}
