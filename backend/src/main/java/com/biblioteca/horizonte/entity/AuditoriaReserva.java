package com.biblioteca.horizonte.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_reservas", indexes = {
    @Index(name = "idx_auditoria_reserva_id", columnList = "reserva_id")
})
public class AuditoriaReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Reserva reserva;


    @Column(name = "usuario", nullable = false, length = 100)
    private String usuario;

    @Column(name = "operacion", nullable = false, length = 50)
    private String operacion;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "resultado", nullable = false, length = 50)
    private String resultado;

    public AuditoriaReserva() {
    }

    public AuditoriaReserva(Reserva reserva, String usuario, String operacion, LocalDateTime fechaHora, String resultado) {
        this.reserva = reserva;
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

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
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
