package com.biblioteca.horizonte.controller;

import com.biblioteca.horizonte.dto.request.CrearReservaRequest;
import com.biblioteca.horizonte.entity.Equipo;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración para ReservaController.
 *
 * Cubre:
 *  - Flujo funcional con autenticación correcta
 *  - CP09: DOCENTE intenta confirmar → 403
 *  - CP09: DOCENTE intenta rechazar → 403
 *  - Usuarios no autenticados → 401
 *  - Excepciones de negocio siguen funcionando (RN-001, RN-003)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ReservaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private EquipoRepository equipoRepository;

    private Long equipoId;

    @BeforeEach
    void setUp() {
        reservaRepository.deleteAll();
        Equipo equipo = equipoRepository.findAll().stream().findFirst().orElseGet(() -> {
            Equipo nuevo = new Equipo("Proyector EPSON Aula Magna");
            return equipoRepository.save(nuevo);
        });
        equipoId = equipo.getId();
    }

    // =========================================================================
    // TESTS FUNCIONALES CON AUTENTICACIÓN CORRECTA
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/reservas: DOCENTE autenticado puede crear una solicitud → 201 PENDIENTE")
    @WithMockUser(username = "docente", roles = "DOCENTE")
    void docenteAutenticadoPuedeCrearSolicitud() throws Exception {
        CrearReservaRequest request = new CrearReservaRequest(
                1L,
                equipoId,
                LocalDate.of(2026, 11, 20),
                "M1"
        );

        mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.docenteId", is(1)))
                .andExpect(jsonPath("$.equipoId", is(equipoId.intValue())))
                .andExpect(jsonPath("$.modulo", is("M1")))
                .andExpect(jsonPath("$.estado", is("PENDIENTE")))
                .andExpect(jsonPath("$.fechaCreacion").isNotEmpty());
    }

    @Test
    @DisplayName("BIBLIOTECARIA autenticada puede confirmar una solicitud → 200 CONFIRMADA")
    @WithMockUser(username = "bibliotecaria", roles = "BIBLIOTECARIA")
    void bibliotecariaPuedeConfirmarSolicitud() throws Exception {
        // Primero crear la solicitud directamente via HTTP Basic
        Long reservaId = crearReservaComoDocente(equipoId, LocalDate.of(2026, 11, 21), "M2");

        mockMvc.perform(post("/api/v1/reservas/" + reservaId + "/confirmar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("CONFIRMADA")));
    }

    @Test
    @DisplayName("BIBLIOTECARIA autenticada puede rechazar una solicitud → 200 RECHAZADA")
    @WithMockUser(username = "bibliotecaria", roles = "BIBLIOTECARIA")
    void bibliotecariaPuedeRechazarSolicitud() throws Exception {
        Long reservaId = crearReservaComoDocente(equipoId, LocalDate.of(2026, 11, 22), "M3");

        mockMvc.perform(post("/api/v1/reservas/" + reservaId + "/rechazar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("RECHAZADA")));
    }

    // =========================================================================
    // CP09 — DOCENTE no puede confirmar ni rechazar (403)
    // =========================================================================

    @Test
    @DisplayName("CP09: DOCENTE intenta confirmar una solicitud → 403 Forbidden")
    @WithMockUser(username = "docente", roles = "DOCENTE")
    void docenteNoDebePoderConfirmar() throws Exception {
        Long reservaId = crearReservaComoDocente(equipoId, LocalDate.of(2026, 11, 23), "M4");

        mockMvc.perform(post("/api/v1/reservas/" + reservaId + "/confirmar"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.codigo", is("ACCESO_DENEGADO")));
    }

    @Test
    @DisplayName("CP09: DOCENTE intenta rechazar una solicitud → 403 Forbidden")
    @WithMockUser(username = "docente", roles = "DOCENTE")
    void docenteNoDebePoderRechazar() throws Exception {
        Long reservaId = crearReservaComoDocente(equipoId, LocalDate.of(2026, 11, 24), "M5");

        mockMvc.perform(post("/api/v1/reservas/" + reservaId + "/rechazar"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.codigo", is("ACCESO_DENEGADO")));
    }

    // =========================================================================
    // Usuarios no autenticados → 401
    // =========================================================================

    @Test
    @DisplayName("Usuario no autenticado que intenta confirmar → 401 Unauthorized")
    void usuarioNoAutenticadoNoDebePoderConfirmar() throws Exception {
        mockMvc.perform(post("/api/v1/reservas/1/confirmar"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.codigo", is("NO_AUTENTICADO")));
    }

    @Test
    @DisplayName("Usuario no autenticado que intenta rechazar → 401 Unauthorized")
    void usuarioNoAutenticadoNoDebePoderRechazar() throws Exception {
        mockMvc.perform(post("/api/v1/reservas/1/rechazar"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.codigo", is("NO_AUTENTICADO")));
    }

    @Test
    @DisplayName("Usuario no autenticado que intenta crear una solicitud → 401 Unauthorized")
    void usuarioNoAutenticadoNoDebePoderCrearSolicitud() throws Exception {
        CrearReservaRequest request = new CrearReservaRequest(1L, equipoId, LocalDate.of(2026, 11, 20), "M1");

        mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.codigo", is("NO_AUTENTICADO")));
    }

    // =========================================================================
    // Validación de request → 400
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/reservas: Falla con 400 Bad Request si faltan campos obligatorios")
    @WithMockUser(username = "docente", roles = "DOCENTE")
    void debeRetornar400CuandoFaltanCampos() throws Exception {
        String jsonVacio = "{}";

        mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonVacio))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.codigo", is("DATOS_INVALIDOS")))
                .andExpect(jsonPath("$.validaciones.docenteId").isNotEmpty())
                .andExpect(jsonPath("$.validaciones.equipoId").isNotEmpty())
                .andExpect(jsonPath("$.validaciones.fecha").isNotEmpty())
                .andExpect(jsonPath("$.validaciones.modulo").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/reservas: Falla con 404 Not Found si el equipo no existe")
    @WithMockUser(username = "docente", roles = "DOCENTE")
    void debeRetornar404CuandoEquipoNoExiste() throws Exception {
        CrearReservaRequest request = new CrearReservaRequest(
                1L,
                99999L,
                LocalDate.of(2026, 11, 20),
                "M1"
        );

        mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.codigo", is("RECURSO_NO_ENCONTRADO")));
    }

    // =========================================================================
    // CP12/CP13 — Las excepciones de negocio siguen funcionando (RN-001, RN-003)
    // =========================================================================

    @Test
    @DisplayName("Flujo Completo RN-001: Solicitud A se confirma; Solicitud B mismo slot → 409 Conflict")
    void flujoCompletoColisionReglaRN001() throws Exception {
        LocalDate fecha = LocalDate.of(2026, 11, 25);
        String modulo = "M2";

        // 1. Crear Solicitud A como docente
        Long idA = crearReservaComoDocente(equipoId, fecha, modulo);

        // 2. Crear Solicitud B para el mismo slot (múltiples pendientes permitidas)
        Long idB = crearReservaComoDocente(equipoId, fecha, modulo);

        // 3. Confirmar Solicitud A como bibliotecaria → 200 OK
        mockMvc.perform(post("/api/v1/reservas/" + idA + "/confirmar")
                        .with(httpBasic("bibliotecaria", "biblio123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("CONFIRMADA")));

        // 4. Intentar confirmar Solicitud B mismo slot → 409 Conflict (RN-001)
        mockMvc.perform(post("/api/v1/reservas/" + idB + "/confirmar")
                        .with(httpBasic("bibliotecaria", "biblio123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.codigo", is("RESERVA_COLISION_CONFIRMADA")))
                .andExpect(jsonPath("$.mensaje", containsString("Ya existe otra reserva confirmada")));

        // 5. Intentar confirmar Solicitud A de nuevo → 409 (RN-003: ya está CONFIRMADA)
        mockMvc.perform(post("/api/v1/reservas/" + idA + "/confirmar")
                        .with(httpBasic("bibliotecaria", "biblio123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("TRANSICION_ESTADO_INVALIDA")));

        // 6. Rechazar Solicitud B → 200 OK
        mockMvc.perform(post("/api/v1/reservas/" + idB + "/rechazar")
                        .with(httpBasic("bibliotecaria", "biblio123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("RECHAZADA")));
    }

    @Test
    @DisplayName("GET /api/v1/reservas: DOCENTE autenticado puede listar solicitudes")
    @WithMockUser(username = "docente", roles = "DOCENTE")
    void docenteAutenticadoPuedeListarSolicitudes() throws Exception {
        mockMvc.perform(get("/api/v1/reservas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/equipos: endpoint público, no requiere autenticación")
    void equiposEsEndpointPublico() throws Exception {
        mockMvc.perform(get("/api/v1/equipos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Crea una reserva autenticando via HTTP Basic con las credenciales de docente.
     * Retorna el ID de la reserva creada.
     */
    private Long crearReservaComoDocente(Long equipoId, LocalDate fecha, String modulo) throws Exception {
        CrearReservaRequest req = new CrearReservaRequest(1L, equipoId, fecha, modulo);

        MvcResult result = mockMvc.perform(post("/api/v1/reservas")
                        .with(httpBasic("docente", "docente123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.id"))
                .longValue();
    }
}
