package com.biblioteca.horizonte.repository;

import com.biblioteca.horizonte.entity.EstadoReserva;
import com.biblioteca.horizonte.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    boolean existsByEquipoIdAndFechaAndModuloAndEstado(
        Long equipoId,
        LocalDate fecha,
        String modulo,
        EstadoReserva estado
    );

    List<Reserva> findByDocenteId(Long docenteId);

    List<Reserva> findByFecha(LocalDate fecha);
}
