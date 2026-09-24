package com.biblioteca.horizonte.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String codigo;
    private String mensaje;
    private String path;
    private Map<String, String> validaciones;

    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(LocalDateTime timestamp, int status, String error, String codigo, String mensaje, String path, Map<String, String> validaciones) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.codigo = codigo;
        this.mensaje = mensaje;
        this.path = path;
        this.validaciones = validaciones;
    }

    public static ErrorResponse of(int status, String error, String codigo, String mensaje, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, error, codigo, mensaje, path, null);
    }

    public static ErrorResponse ofValidation(int status, String error, String codigo, String mensaje, String path, Map<String, String> validaciones) {
        return new ErrorResponse(LocalDateTime.now(), status, error, codigo, mensaje, path, validaciones);
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getValidaciones() {
        return validaciones;
    }

    public void setValidaciones(Map<String, String> validaciones) {
        this.validaciones = validaciones;
    }
}
