package com.biblioteca.horizonte.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class CrearReservaRequest {

    @NotNull(message = "El identificador del docente es obligatorio")
    private Long docenteId;

    @NotNull(message = "El identificador del equipo es obligatorio")
    private Long equipoId;

    @NotNull(message = "La fecha de reserva es obligatoria")
    private LocalDate fecha;

    @NotBlank(message = "El módulo horario es obligatorio")
    private String modulo;

    public CrearReservaRequest() {
    }

    public CrearReservaRequest(Long docenteId, Long equipoId, LocalDate fecha, String modulo) {
        this.docenteId = docenteId;
        this.equipoId = equipoId;
        this.fecha = fecha;
        this.modulo = modulo;
    }

    public Long getDocenteId() {
        return docenteId;
    }

    public void setDocenteId(Long docenteId) {
        this.docenteId = docenteId;
    }

    public Long getEquipoId() {
        return equipoId;
    }

    public void setEquipoId(Long equipoId) {
        this.equipoId = equipoId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getModulo() {
        return modulo;
    }

    public void setModulo(String modulo) {
        this.modulo = modulo;
    }
}
