package com.biblioteca.horizonte.service;

import com.biblioteca.horizonte.dto.request.CrearReservaRequest;
import com.biblioteca.horizonte.dto.response.ReservaResponse;
import com.biblioteca.horizonte.entity.Equipo;
import com.biblioteca.horizonte.entity.EstadoReserva;
import com.biblioteca.horizonte.entity.Reserva;
import com.biblioteca.horizonte.exception.InvalidStateTransitionException;
import com.biblioteca.horizonte.exception.ReservaConflictException;
import com.biblioteca.horizonte.exception.ResourceNotFoundException;
import com.biblioteca.horizonte.repository.EquipoRepository;
import com.biblioteca.horizonte.repository.ReservaRepository;
import com.biblioteca.horizonte.service.impl.ReservaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private EquipoRepository equipoRepository;

    @InjectMocks
    private ReservaServiceImpl reservaService;

    private Equipo equipoX;
    private LocalDate fechaY;
    private String moduloZ;

    @BeforeEach
    void setUp() {
        equipoX = new Equipo(10L, "Proyector EPSON Aula Magna");
        fechaY = LocalDate.of(2026, 10, 15);
        moduloZ = "M1";
    }

    @Test
    @DisplayName("REQ-001 / REQ-002: Debe crear una solicitud y registrarla en estado inicial PENDIENTE")
    void debeCrearSolicitudEnEstadoPendiente() {
        CrearReservaRequest request = new CrearReservaRequest(1L, 10L, fechaY, moduloZ);

        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipoX));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> {
            Reserva r = invocation.getArgument(0);
            r.setId(101L);
            return r;
        });

        ReservaResponse response = reservaService.crearSolicitud(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getEstado()).isEqualTo(EstadoReserva.PENDIENTE);
        assertThat(response.getEquipoNombre()).isEqualTo("Proyector EPSON Aula Magna");
        assertThat(response.getFechaCreacion()).isNotNull();

        verify(equipoRepository).findById(10L);
        verify(reservaRepository).save(any(Reserva.class));
    }

    @Test
    @DisplayName("REQ-003: Solicitud A (equipo X + fecha Y + módulo Z) puede confirmarse si no hay colisión previa")
    void debeConfirmarSolicitudExitosamenteCuandoNoHayColision() {
        Reserva solicitudA = new Reserva(101L, 1L, equipoX, fechaY, moduloZ, EstadoReserva.PENDIENTE, LocalDateTime.now());

        when(reservaRepository.findById(101L)).thenReturn(Optional.of(solicitudA));
        when(reservaRepository.existsByEquipoIdAndFechaAndModuloAndEstado(10L, fechaY, moduloZ, EstadoReserva.CONFIRMADA))
                .thenReturn(false);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservaResponse response = reservaService.confirmarSolicitud(101L);

        assertThat(response.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        verify(reservaRepository).save(solicitudA);
    }

    @Test
    @DisplayName("REQ-005 / RN-001: Solicitud B (mismo equipo X + fecha Y + módulo Z) NO puede confirmarse si A ya está confirmada")
    void noDebeConfirmarSolicitudSiYaExisteOtraConfirmadaParaElMismoSlot() {
        // Solicitud B se encuentra en PENDIENTE
        Reserva solicitudB = new Reserva(102L, 2L, equipoX, fechaY, moduloZ, EstadoReserva.PENDIENTE, LocalDateTime.now());

        when(reservaRepository.findById(102L)).thenReturn(Optional.of(solicitudB));
        // Ya existe una reserva CONFIRMADA (Solicitud A) para ese slot
        when(reservaRepository.existsByEquipoIdAndFechaAndModuloAndEstado(10L, fechaY, moduloZ, EstadoReserva.CONFIRMADA))
                .thenReturn(true);

        assertThatThrownBy(() -> reservaService.confirmarSolicitud(102L))
                .isInstanceOf(ReservaConflictException.class)
                .hasMessageContaining("Ya existe otra reserva confirmada");

        // Verificamos que no se haya guardado ninguna actualización para la solicitud B
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    @DisplayName("REQ-004: Debe rechazar una solicitud en estado PENDIENTE transicionando a RECHAZADA")
    void debeRechazarSolicitudExitosamente() {
        Reserva solicitud = new Reserva(101L, 1L, equipoX, fechaY, moduloZ, EstadoReserva.PENDIENTE, LocalDateTime.now());

        when(reservaRepository.findById(101L)).thenReturn(Optional.of(solicitud));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservaResponse response = reservaService.rechazarSolicitud(101L);

        assertThat(response.getEstado()).isEqualTo(EstadoReserva.RECHAZADA);
        verify(reservaRepository).save(solicitud);
    }

    @Test
    @DisplayName("RN-003: No debe permitir confirmar una solicitud que ya está CONFIRMADA")
    void noDebeConfirmarSolicitudSiYaEstaConfirmada() {
        Reserva solicitudYaConfirmada = new Reserva(101L, 1L, equipoX, fechaY, moduloZ, EstadoReserva.CONFIRMADA, LocalDateTime.now());

        when(reservaRepository.findById(101L)).thenReturn(Optional.of(solicitudYaConfirmada));

        assertThatThrownBy(() -> reservaService.confirmarSolicitud(101L))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Solo se pueden confirmar solicitudes en estado PENDIENTE");

        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    @DisplayName("RN-003: No debe permitir confirmar una solicitud que ya está RECHAZADA")
    void noDebeConfirmarSolicitudSiYaEstaRechazada() {
        Reserva solicitudRechazada = new Reserva(101L, 1L, equipoX, fechaY, moduloZ, EstadoReserva.RECHAZADA, LocalDateTime.now());

        when(reservaRepository.findById(101L)).thenReturn(Optional.of(solicitudRechazada));

        assertThatThrownBy(() -> reservaService.confirmarSolicitud(101L))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Solo se pueden confirmar solicitudes en estado PENDIENTE");

        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    @DisplayName("RN-003: No debe permitir rechazar una solicitud que ya está CONFIRMADA")
    void noDebeRechazarSolicitudSiYaEstaConfirmada() {
        Reserva solicitudConfirmada = new Reserva(101L, 1L, equipoX, fechaY, moduloZ, EstadoReserva.CONFIRMADA, LocalDateTime.now());

        when(reservaRepository.findById(101L)).thenReturn(Optional.of(solicitudConfirmada));

        assertThatThrownBy(() -> reservaService.rechazarSolicitud(101L))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Solo se pueden rechazar solicitudes en estado PENDIENTE");

        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el equipo no existe al crear solicitud")
    void debeLanzarExcepcionSiEquipoNoExiste() {
        CrearReservaRequest request = new CrearReservaRequest(1L, 999L, fechaY, moduloZ);

        when(equipoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaService.crearSolicitud(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("El equipo con ID 999 no existe");
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si la reserva no existe al confirmar")
    void debeLanzarExcepcionSiReservaNoExisteAlConfirmar() {
        when(reservaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaService.confirmarSolicitud(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No se encontró la reserva con ID: 999");
    }

    @Test
    @DisplayName("CP10 / RF09: listarMisSolicitudes debe invocar findByDocenteId y mapear resultados")
    void debeListarMisSolicitudesFiltradasPorDocenteId() {
        Reserva r1 = new Reserva(101L, 1L, equipoX, fechaY, moduloZ, EstadoReserva.PENDIENTE, LocalDateTime.now());
        when(reservaRepository.findByDocenteId(1L)).thenReturn(List.of(r1));

        List<ReservaResponse> respuestas = reservaService.listarMisSolicitudes(1L);

        assertThat(respuestas).hasSize(1);
        assertThat(respuestas.get(0).getDocenteId()).isEqualTo(1L);
        assertThat(respuestas.get(0).getId()).isEqualTo(101L);
        verify(reservaRepository).findByDocenteId(1L);
    }

    @Test
    @DisplayName("CP14 / RF10: listarConfirmadas con equipo y fecha debe filtrar solo CONFIRMADAS")
    void debeListarConfirmadasPorEquipoYFecha() {
        Reserva rConfirmada = new Reserva(101L, 1L, equipoX, fechaY, moduloZ, EstadoReserva.CONFIRMADA, LocalDateTime.now());
        when(reservaRepository.findByEquipoIdAndFechaAndEstado(10L, fechaY, EstadoReserva.CONFIRMADA))
                .thenReturn(List.of(rConfirmada));

        List<ReservaResponse> respuestas = reservaService.listarConfirmadas(10L, fechaY);

        assertThat(respuestas).hasSize(1);
        assertThat(respuestas.get(0).getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(respuestas.get(0).getEquipoId()).isEqualTo(10L);
        assertThat(respuestas.get(0).getFecha()).isEqualTo(fechaY);
        verify(reservaRepository).findByEquipoIdAndFechaAndEstado(10L, fechaY, EstadoReserva.CONFIRMADA);
    }

    @Test
    @DisplayName("CP14: listarConfirmadas solo por fecha debe consultar findByFechaAndEstado")
    void debeListarConfirmadasSoloPorFecha() {
        Reserva rConfirmada = new Reserva(102L, 2L, equipoX, fechaY, "M2", EstadoReserva.CONFIRMADA, LocalDateTime.now());
        when(reservaRepository.findByFechaAndEstado(fechaY, EstadoReserva.CONFIRMADA))
                .thenReturn(List.of(rConfirmada));

        List<ReservaResponse> respuestas = reservaService.listarConfirmadas(null, fechaY);

        assertThat(respuestas).hasSize(1);
        assertThat(respuestas.get(0).getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        verify(reservaRepository).findByFechaAndEstado(fechaY, EstadoReserva.CONFIRMADA);
    }
}
