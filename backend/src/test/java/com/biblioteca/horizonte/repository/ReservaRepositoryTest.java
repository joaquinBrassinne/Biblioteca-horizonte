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
}
