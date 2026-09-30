package com.biblioteca.horizonte.integration;

import com.biblioteca.horizonte.dto.request.CrearReservaRequest;
import com.biblioteca.horizonte.entity.AuditoriaReserva;
import com.biblioteca.horizonte.entity.Equipo;
import com.biblioteca.horizonte.repository.AuditoriaReservaRepository;
import com.biblioteca.horizonte.repository.EquipoRepository;
import com.biblioteca.horizonte.repository.ReservaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class AuditoriaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditoriaReservaRepository auditoriaReservaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private EquipoRepository equipoRepository;

    private Long equipoId;

    @BeforeEach
    void setUp() {
        auditoriaReservaRepository.deleteAll();
        reservaRepository.deleteAll();

        Equipo equipo = equipoRepository.findAll().stream().findFirst().orElseGet(() -> {
            Equipo nuevo = new Equipo("Notebook HP Laboratorio");
            return equipoRepository.save(nuevo);
        });
        equipoId = equipo.getId();
    }

    private Long crearSolicitud(Long docenteId, LocalDate fecha, String modulo) throws Exception {
        CrearReservaRequest req = new CrearReservaRequest(docenteId, equipoId, fecha, modulo);
        MvcResult res = mockMvc.perform(post("/api/v1/reservas")
                        .with(httpBasic("docente", "docente123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    @DisplayName("CP15: Confirmación válida registra fecha, hora, usuario y operación en auditoría")
    void cp15_confirmacionGeneraAuditoriaCorrecta() throws Exception {
        Long reservaId = crearSolicitud(1L, LocalDate.of(2026, 11, 22), "M1");
        LocalDateTime antesDeOperacion = LocalDateTime.now().minusSeconds(1);

        // Operación de confirmación autenticada como bibliotecaria
        mockMvc.perform(post("/api/v1/reservas/" + reservaId + "/confirmar")
                        .with(httpBasic("bibliotecaria", "biblio123")))
                .andExpect(status().isOk());

        LocalDateTime despuesDeOperacion = LocalDateTime.now().plusSeconds(1);

        // Verificación en repositorio de auditoría
        List<AuditoriaReserva> auditorias = auditoriaReservaRepository.findByReservaId(reservaId);
        assertThat(auditorias).hasSize(1);

        AuditoriaReserva audit = auditorias.get(0);
        assertThat(audit.getReserva().getId()).isEqualTo(reservaId);
        assertThat(audit.getUsuario()).isEqualTo("bibliotecaria");
        assertThat(audit.getOperacion()).isEqualTo("CONFIRMAR");
        assertThat(audit.getResultado()).isEqualTo("EXITOSO");
        assertThat(audit.getFechaHora()).isAfterOrEqualTo(antesDeOperacion);
        assertThat(audit.getFechaHora()).isBeforeOrEqualTo(despuesDeOperacion);
    }

    @Test
    @DisplayName("CP15: Rechazo válido registra fecha, hora, usuario y operación en auditoría")
    void cp15_rechazoGeneraAuditoriaCorrecta() throws Exception {
        Long reservaId = crearSolicitud(1L, LocalDate.of(2026, 11, 23), "M2");

        // Operación de rechazo autenticada como bibliotecaria
        mockMvc.perform(post("/api/v1/reservas/" + reservaId + "/rechazar")
                        .with(httpBasic("bibliotecaria", "biblio123")))
                .andExpect(status().isOk());

        List<AuditoriaReserva> auditorias = auditoriaReservaRepository.findByReservaId(reservaId);
        assertThat(auditorias).hasSize(1);

        AuditoriaReserva audit = auditorias.get(0);
        assertThat(audit.getReserva().getId()).isEqualTo(reservaId);
        assertThat(audit.getUsuario()).isEqualTo("bibliotecaria");
        assertThat(audit.getOperacion()).isEqualTo("RECHAZAR");
        assertThat(audit.getResultado()).isEqualTo("EXITOSO");
        assertThat(audit.getFechaHora()).isNotNull();
    }

    @Test
    @DisplayName("CP15: Operación fallida por colisión/conflicto (409) NO genera auditoría de confirmación")
    void cp15_operacionFallidaPorColisionNoGeneraAuditoria() throws Exception {
        LocalDate fecha = LocalDate.of(2026, 11, 24);
        String modulo = "M3";

        // Solicitud 1 confirmada
        Long id1 = crearSolicitud(1L, fecha, modulo);
        mockMvc.perform(post("/api/v1/reservas/" + id1 + "/confirmar")
                        .with(httpBasic("bibliotecaria", "biblio123")))
                .andExpect(status().isOk());

        // Solicitud 2 para mismo slot
        Long id2 = crearSolicitud(2L, fecha, modulo);

        // Intento de confirmar solicitud 2 -> falla con 409 Conflict
        mockMvc.perform(post("/api/v1/reservas/" + id2 + "/confirmar")
                        .with(httpBasic("bibliotecaria", "biblio123")))
                .andExpect(status().isConflict());

        // Solicitud 1 tiene su auditoría
        assertThat(auditoriaReservaRepository.findByReservaId(id1)).hasSize(1);

        // Solicitud 2 que falló NO debe tener ningún registro de auditoría de confirmación
        assertThat(auditoriaReservaRepository.findByReservaId(id2)).isEmpty();
    }

    @Test
    @DisplayName("CP15: Operación fallida por transición inválida (409) NO genera auditoría falsa")
    void cp15_operacionFallidaPorTransicionInvalidaNoGeneraAuditoria() throws Exception {
        Long id = crearSolicitud(1L, LocalDate.of(2026, 11, 25), "M4");

        // Rechazar
        mockMvc.perform(post("/api/v1/reservas/" + id + "/rechazar")
                        .with(httpBasic("bibliotecaria", "biblio123")))
                .andExpect(status().isOk());

        // Intentar confirmar reserva ya rechazada -> 409 Conflict
        mockMvc.perform(post("/api/v1/reservas/" + id + "/confirmar")
                        .with(httpBasic("bibliotecaria", "biblio123")))
                .andExpect(status().isConflict());

        // Debe conservar únicamente la auditoría del rechazo original
        List<AuditoriaReserva> registros = auditoriaReservaRepository.findByReservaId(id);
        assertThat(registros).hasSize(1);
        assertThat(registros.get(0).getOperacion()).isEqualTo("RECHAZAR");
    }

    @Test
    @DisplayName("CP15: Operación rechazada por falta de autorización (403) NO genera auditoría")
    void cp15_operacionRechazadaPorAutorizacionNoGeneraAuditoria() throws Exception {
        Long id = crearSolicitud(1L, LocalDate.of(2026, 11, 26), "M5");

        // Docente intenta confirmar -> 403 Forbidden
        mockMvc.perform(post("/api/v1/reservas/" + id + "/confirmar")
                        .with(httpBasic("docente", "docente123")))
                .andExpect(status().isForbidden());

        // No debe haberse registrado ninguna auditoría
        assertThat(auditoriaReservaRepository.findByReservaId(id)).isEmpty();
    }

    @Test
    @DisplayName("HU11: Consulta de trazabilidad de una reserva vía endpoint GET /reservas/{id}/auditoria")
    void hu11_consultaDeTrazabilidadPorEndpoint() throws Exception {
        Long id = crearSolicitud(1L, LocalDate.of(2026, 11, 27), "M1");

        mockMvc.perform(post("/api/v1/reservas/" + id + "/confirmar")
                        .with(httpBasic("bibliotecaria", "biblio123")))
                .andExpect(status().isOk());

        // Bibliotecaria consulta el historial de auditoría
        mockMvc.perform(get("/api/v1/reservas/" + id + "/auditoria")
                        .with(httpBasic("bibliotecaria", "biblio123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].reservaId", is(id.intValue())))
                .andExpect(jsonPath("$[0].usuario", is("bibliotecaria")))
                .andExpect(jsonPath("$[0].operacion", is("CONFIRMAR")))
                .andExpect(jsonPath("$[0].resultado", is("EXITOSO")))
                .andExpect(jsonPath("$[0].fechaHora").isNotEmpty());

        // Docente no tiene rol de bibliotecaria -> 403 Forbidden
        mockMvc.perform(get("/api/v1/reservas/" + id + "/auditoria")
                        .with(httpBasic("docente", "docente123")))
                .andExpect(status().isForbidden());
    }
}
