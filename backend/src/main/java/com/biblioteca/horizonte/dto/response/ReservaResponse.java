package com.biblioteca.horizonte.dto.response;

import com.biblioteca.horizonte.entity.EstadoReserva;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReservaResponse {

    private Long id;
    private Long docenteId;
    private Long equipoId;
    private String equipoNombre;
    private LocalDate fecha;
    private String modulo;
    private EstadoReserva estado;
    private LocalDateTime fechaCreacion;

    public ReservaResponse() {
    }

    public ReservaResponse(Long id, Long docenteId, Long equipoId, String equipoNombre, LocalDate fecha, String modulo, EstadoReserva estado, LocalDateTime fechaCreacion) {
        this.id = id;
        this.docenteId = docenteId;
        this.equipoId = equipoId;
        this.equipoNombre = equipoNombre;
        this.fecha = fecha;
        this.modulo = modulo;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getEquipoNombre() {
        return equipoNombre;
    }

    public void setEquipoNombre(String equipoNombre) {
        this.equipoNombre = equipoNombre;
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

    public EstadoReserva getEstado() {
        return estado;
    }

    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
