package com.biblioteca.horizonte.integration;

import com.biblioteca.horizonte.dto.request.CrearReservaRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class ReservaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Escenario Completo: CASO 1 (Crear PENDIENTE), CASO 2 (Confirmar), CASO 3 (Conflicto 409 al intentar confirmar colisión)")
    void testFlujoCompletoConcurrenciaYConflicto() throws Exception {
        LocalDate fecha = LocalDate.of(2026, 11, 20);
        String modulo = "M1";
        Long equipoId = 10L; // Precargado en data-h2.sql
        Long docente1 = 1L;
        Long docente2 = 2L;

        // ==========================================
        // CASO 1: Crear primera solicitud
        // ==========================================
        CrearReservaRequest req1 = new CrearReservaRequest(docente1, equipoId, fecha, modulo);

        MvcResult res1 = mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.equipoId").value(equipoId))
                .andExpect(jsonPath("$.modulo").value(modulo))
                .andReturn();

        String response1Json = res1.getResponse().getContentAsString();
        Long reserva1Id = objectMapper.readTree(response1Json).get("id").asLong();

        // ==========================================
        // CASO 2: Confirmar la primera solicitud
        // ==========================================
        mockMvc.perform(post("/api/v1/reservas/{id}/confirmar", reserva1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reserva1Id))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        // ==========================================
        // CASO 3: Crear segunda solicitud para el MISMO equipo + fecha + módulo
        // ==========================================
        CrearReservaRequest req2 = new CrearReservaRequest(docente2, equipoId, fecha, modulo);

        MvcResult res2 = mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.estado").value("PENDIENTE")) // Se permite crear en PENDIENTE
                .andReturn();

        Long reserva2Id = objectMapper.readTree(res2.getResponse().getContentAsString()).get("id").asLong();

        // Intentar CONFIRMAR la segunda solicitud -> Debe fallar con 409 Conflict por regla fundamental RN-001
        mockMvc.perform(post("/api/v1/reservas/{id}/confirmar", reserva2Id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.codigo").value("RESERVA_COLISION_CONFIRMADA"))
                .andExpect(jsonPath("$.mensaje", containsString("Ya existe otra reserva confirmada")));

        // Verificar que la segunda reserva permanezca en PENDIENTE y NO se haya confirmado
        mockMvc.perform(get("/api/v1/reservas/{id}", reserva2Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        // ==========================================
        // CASO 4: Rechazar la segunda solicitud
        // ==========================================
        mockMvc.perform(post("/api/v1/reservas/{id}/rechazar", reserva2Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"));

        // Intentar confirmar una solicitud ya RECHAZADA -> 409 Conflict por transición inválida
        mockMvc.perform(post("/api/v1/reservas/{id}/confirmar", reserva2Id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICION_ESTADO_INVALIDA"));
    }
}
