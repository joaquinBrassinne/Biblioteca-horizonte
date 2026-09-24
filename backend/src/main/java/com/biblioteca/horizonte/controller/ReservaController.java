package com.biblioteca.horizonte.controller;

import com.biblioteca.horizonte.dto.request.CrearReservaRequest;
import com.biblioteca.horizonte.dto.response.ReservaResponse;
import com.biblioteca.horizonte.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> crearSolicitud(@Valid @RequestBody CrearReservaRequest request) {
        ReservaResponse response = reservaService.crearSolicitud(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ReservaResponse>> listarTodas() {
        return ResponseEntity.ok(reservaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.obtenerPorId(id));
    }

    @PostMapping("/{id}/confirmar")
    public ResponseEntity<ReservaResponse> confirmarSolicitud(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.confirmarSolicitud(id));
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<ReservaResponse> rechazarSolicitud(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.rechazarSolicitud(id));
    }
}
