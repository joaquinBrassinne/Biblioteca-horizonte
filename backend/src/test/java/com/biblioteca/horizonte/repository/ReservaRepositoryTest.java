package com.biblioteca.horizonte.repository;

import com.biblioteca.horizonte.entity.Equipo;
import com.biblioteca.horizonte.entity.EstadoReserva;
import com.biblioteca.horizonte.entity.Reserva;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("dev")
class ReservaRepositoryTest {

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private EquipoRepository equipoRepository;

    @Test
    void shouldPersistAndRetrieveReserva() {
        Equipo equipo = new Equipo("Proyector Test");
        equipo = equipoRepository.save(equipo);

        Reserva reserva = new Reserva(
                null,
                1L,
                equipo,
                LocalDate.of(2026, 10, 15),
                "M1",
                EstadoReserva.PENDIENTE,
                LocalDateTime.now()
        );
        reserva = reservaRepository.save(reserva);

        assertThat(reserva.getId()).isNotNull();

        boolean exists = reservaRepository.existsByEquipoIdAndFechaAndModuloAndEstado(
                equipo.getId(),
                LocalDate.of(2026, 10, 15),
                "M1",
                EstadoReserva.PENDIENTE
        );
        assertThat(exists).isTrue();

        List<Reserva> docenteReservas = reservaRepository.findByDocenteId(1L);
        assertThat(docenteReservas).isNotEmpty();
    }

    @Test
    void shouldFilterConfirmedReservasByEquipoAndFecha() {
        Equipo equipo = equipoRepository.save(new Equipo("Notebook Test"));
        LocalDate fecha = LocalDate.of(2026, 10, 20);

        // Reserva CONFIRMADA
        Reserva confirmada = new Reserva(null, 1L, equipo, fecha, "M1", EstadoReserva.CONFIRMADA, LocalDateTime.now());
        reservaRepository.save(confirmada);

        // Reserva PENDIENTE mismo equipo y fecha
        Reserva pendiente = new Reserva(null, 2L, equipo, fecha, "M2", EstadoReserva.PENDIENTE, LocalDateTime.now());
        reservaRepository.save(pendiente);

        // Reserva CONFIRMADA otra fecha
        Reserva otraFecha = new Reserva(null, 1L, equipo, LocalDate.of(2026, 10, 21), "M1", EstadoReserva.CONFIRMADA, LocalDateTime.now());
        reservaRepository.save(otraFecha);

        List<Reserva> resultado = reservaRepository.findByEquipoIdAndFechaAndEstado(equipo.getId(), fecha, EstadoReserva.CONFIRMADA);
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(resultado.get(0).getModulo()).isEqualTo("M1");

        List<Reserva> soloFecha = reservaRepository.findByFechaAndEstado(fecha, EstadoReserva.CONFIRMADA);
        assertThat(soloFecha).hasSize(1);
    }
}
