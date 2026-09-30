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

    @Autowired(required = false)
    private com.biblioteca.horizonte.repository.AuditoriaReservaRepository auditoriaReservaRepository;

    private Long equipoId;

    @BeforeEach
    void setUp() {
        if (auditoriaReservaRepository != null) {
            auditoriaReservaRepository.deleteAll();
        }
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
    // CP10 — DOCENTE consulta sus propias solicitudes
    // =========================================================================

    @Test
    @DisplayName("CP10: DOCENTE A consulta sus propias solicitudes en /mis-solicitudes → 200 OK, solo ve las suyas")
    void cp10_docenteConsultaSusPropiasSolicitudes() throws Exception {
        // Docente 1 crea reserva
        Long idReservaDocente1 = crearReservaComoDocente(equipoId, LocalDate.of(2026, 12, 1), "M1");
        // Docente 2 crea reserva
        Long idReservaDocente2 = crearReservaComoDocente2(equipoId, LocalDate.of(2026, 12, 2), "M2");

        // Docente 1 consulta /mis-solicitudes
        mockMvc.perform(get("/api/v1/reservas/mis-solicitudes")
                        .with(httpBasic("docente", "docente123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(idReservaDocente1.intValue())))
                .andExpect(jsonPath("$[0].docenteId", is(1)));

        // Docente 2 consulta /mis-solicitudes
        mockMvc.perform(get("/api/v1/reservas/mis-solicitudes")
                        .with(httpBasic("docente2", "docente123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(idReservaDocente2.intValue())))
                .andExpect(jsonPath("$[0].docenteId", is(2)));
    }

    @Test
    @DisplayName("CP10: DOCENTE A consulta /api/v1/reservas → 200 OK, filtrado automático a sus solicitudes")
    void cp10_docenteConsultaReservasGeneralesSoloVeLasSuyas() throws Exception {
        Long idReservaDocente1 = crearReservaComoDocente(equipoId, LocalDate.of(2026, 12, 3), "M1");
        crearReservaComoDocente2(equipoId, LocalDate.of(2026, 12, 4), "M2");

        mockMvc.perform(get("/api/v1/reservas")
                        .with(httpBasic("docente", "docente123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(idReservaDocente1.intValue())))
                .andExpect(jsonPath("$[0].docenteId", is(1)));
    }

    // =========================================================================
    // CP11 — DOCENTE NO puede consultar solicitudes pertenecientes a otro docente
    // =========================================================================

    @Test
    @DisplayName("CP11: DOCENTE 1 intenta acceder a detalle de solicitud de DOCENTE 2 → 403 Forbidden")
    void cp11_docenteNoPuedeAccederADetalleDeOtroDocente() throws Exception {
        Long idReservaDocente2 = crearReservaComoDocente2(equipoId, LocalDate.of(2026, 12, 5), "M3");

        mockMvc.perform(get("/api/v1/reservas/" + idReservaDocente2)
                        .with(httpBasic("docente", "docente123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.codigo", is("ACCESO_DENEGADO")));
    }

    @Test
    @DisplayName("CP11: DOCENTE 1 intenta consultar solicitudes de DOCENTE 2 via parámetro ?docenteId=2 → 403 Forbidden")
    void cp11_docenteNoPuedeFiltrarPorIdDeOtroDocente() throws Exception {
        crearReservaComoDocente2(equipoId, LocalDate.of(2026, 12, 6), "M4");

        mockMvc.perform(get("/api/v1/reservas")
                        .param("docenteId", "2")
                        .with(httpBasic("docente", "docente123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.codigo", is("ACCESO_DENEGADO")));
    }

    @Test
    @DisplayName("CP11: BIBLIOTECARIA puede consultar cualquier solicitud (acceso administrativo) → 200 OK")
    void cp11_bibliotecariaPuedeConsultarCualquierSolicitud() throws Exception {
        Long idReservaDocente1 = crearReservaComoDocente(equipoId, LocalDate.of(2026, 12, 7), "M5");

        mockMvc.perform(get("/api/v1/reservas/" + idReservaDocente1)
                        .with(httpBasic("bibliotecaria", "biblio123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(idReservaDocente1.intValue())))
                .andExpect(jsonPath("$.docenteId", is(1)));
    }

    @Test
    @DisplayName("CP11: Usuario no autenticado que intenta consultar /mis-solicitudes → 401 Unauthorized")
    void cp11_usuarioNoAutenticadoEnMisSolicitudesRetorna401() throws Exception {
        mockMvc.perform(get("/api/v1/reservas/mis-solicitudes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.codigo", is("NO_AUTENTICADO")));
    }

    @Test
    @DisplayName("CP11: BIBLIOTECARIA intenta acceder a /mis-solicitudes → 403 Forbidden (rol docente requerido)")
    void cp11_bibliotecariaEnMisSolicitudesRetorna403() throws Exception {
        mockMvc.perform(get("/api/v1/reservas/mis-solicitudes")
                        .with(httpBasic("bibliotecaria", "biblio123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.codigo", is("ACCESO_DENEGADO")));
    }

    // =========================================================================
    // CP14 — Consulta de disponibilidad y reservas CONFIRMADAS por recurso y fecha
    // =========================================================================

    @Test
    @DisplayName("CP14: Consulta de reservas CONFIRMADAS por recurso y fecha → solo retorna CONFIRMADAS")
    void cp14_consultaReservasConfirmadasPorRecursoYFecha() throws Exception {
        LocalDate fecha = LocalDate.of(2026, 12, 10);

        // 1. Solicitud confirmada para equipoId en fecha
        Long idConfirmada = crearReservaComoDocente(equipoId, fecha, "M1");
        mockMvc.perform(post("/api/v1/reservas/" + idConfirmada + "/confirmar")
                        .with(httpBasic("bibliotecaria", "biblio123")))
                .andExpect(status().isOk());

        // 2. Solicitud que queda PENDIENTE para mismo equipoId y fecha
        crearReservaComoDocente(equipoId, fecha, "M2");

        // 3. Solicitud que queda RECHAZADA para mismo equipoId y fecha
        Long idRechazada = crearReservaComoDocente(equipoId, fecha, "M3");
        mockMvc.perform(post("/api/v1/reservas/" + idRechazada + "/rechazar")
                        .with(httpBasic("bibliotecaria", "biblio123")))
                .andExpect(status().isOk());

        // 4. Solicitud confirmada para OTRA fecha
        Long idOtraFecha = crearReservaComoDocente(equipoId, LocalDate.of(2026, 12, 15), "M1");
        mockMvc.perform(post("/api/v1/reservas/" + idOtraFecha + "/confirmar")
                        .with(httpBasic("bibliotecaria", "biblio123")))
                .andExpect(status().isOk());

        // Consultar confirmadas filtrando por equipoId y fecha
        mockMvc.perform(get("/api/v1/reservas/confirmadas")
                        .param("equipoId", equipoId.toString())
                        .param("fecha", "2026-12-10")
                        .with(httpBasic("docente", "docente123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(idConfirmada.intValue())))
                .andExpect(jsonPath("$[0].estado", is("CONFIRMADA")))
                .andExpect(jsonPath("$[0].modulo", is("M1")))
                .andExpect(jsonPath("$[0].fecha", is("2026-12-10")));
    }

    @Test
    @DisplayName("CP14: Consulta de reservas CONFIRMADAS solo por fecha → retorna confirmadas de esa fecha")
    void cp14_consultaReservasConfirmadasSoloPorFecha() throws Exception {
        LocalDate fecha = LocalDate.of(2026, 12, 20);

        Long idConf = crearReservaComoDocente(equipoId, fecha, "M1");
        mockMvc.perform(post("/api/v1/reservas/" + idConf + "/confirmar")
                        .with(httpBasic("bibliotecaria", "biblio123")))
                .andExpect(status().isOk());

        // Solicitud pendiente en la misma fecha no debe salir
        crearReservaComoDocente(equipoId, fecha, "M2");

        mockMvc.perform(get("/api/v1/reservas/confirmadas")
                        .param("fecha", "2026-12-20")
                        .with(httpBasic("docente", "docente123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].estado", is("CONFIRMADA")))
                .andExpect(jsonPath("$[0].id", is(idConf.intValue())));
    }

    @Test
    @DisplayName("CP14: Consulta de reservas confirmadas sin autenticación → 401 Unauthorized")
    void cp14_consultaConfirmadasSinAutenticacionRetorna401() throws Exception {
        mockMvc.perform(get("/api/v1/reservas/confirmadas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.codigo", is("NO_AUTENTICADO")));
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

    /**
     * Crea una reserva autenticando via HTTP Basic con las credenciales de docente2.
     * Retorna el ID de la reserva creada.
     */
    private Long crearReservaComoDocente2(Long equipoId, LocalDate fecha, String modulo) throws Exception {
        CrearReservaRequest req = new CrearReservaRequest(2L, equipoId, fecha, modulo);

        MvcResult result = mockMvc.perform(post("/api/v1/reservas")
                        .with(httpBasic("docente2", "docente123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.id"))
                .longValue();
    }
}
