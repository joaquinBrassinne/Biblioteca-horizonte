package com.biblioteca.horizonte.dto.response;

import java.time.LocalDateTime;

public class AuditoriaResponse {

    private Long id;
    private Long reservaId;
    private String usuario;
    private String operacion;
    private LocalDateTime fechaHora;
    private String resultado;

    public AuditoriaResponse() {
    }

    public AuditoriaResponse(Long id, Long reservaId, String usuario, String operacion, LocalDateTime fechaHora, String resultado) {
        this.id = id;
        this.reservaId = reservaId;
        this.usuario = usuario;
        this.operacion = operacion;
        this.fechaHora = fechaHora;
        this.resultado = resultado;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReservaId() {
        return reservaId;
    }

    public void setReservaId(Long reservaId) {
        this.reservaId = reservaId;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getOperacion() {
        return operacion;
    }

    public void setOperacion(String operacion) {
        this.operacion = operacion;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }
}
