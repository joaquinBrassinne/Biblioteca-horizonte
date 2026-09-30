package com.biblioteca.horizonte.service;

import com.biblioteca.horizonte.dto.response.AuditoriaResponse;
import com.biblioteca.horizonte.entity.Reserva;

import java.util.List;

public interface AuditoriaService {

    void registrarAuditoria(Reserva reserva, String operacion, String resultado);

    List<AuditoriaResponse> obtenerPorReservaId(Long reservaId);

    List<AuditoriaResponse> listarTodas();
}
