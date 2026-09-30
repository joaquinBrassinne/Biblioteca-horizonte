package com.biblioteca.horizonte.controller;

import com.biblioteca.horizonte.dto.request.CrearReservaRequest;
import com.biblioteca.horizonte.dto.response.ReservaResponse;
import com.biblioteca.horizonte.service.ReservaService;
import com.biblioteca.horizonte.security.DocenteResolver;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import com.biblioteca.horizonte.dto.response.AuditoriaResponse;
import com.biblioteca.horizonte.service.AuditoriaService;

@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaController {

    private final ReservaService reservaService;
    private final DocenteResolver docenteResolver;
    private final AuditoriaService auditoriaService;

    public ReservaController(ReservaService reservaService) {
        this(reservaService, new DocenteResolver(), null);
    }

    public ReservaController(ReservaService reservaService, DocenteResolver docenteResolver) {
        this(reservaService, docenteResolver, null);
    }

    @Autowired
    public ReservaController(ReservaService reservaService,
                             DocenteResolver docenteResolver,
                             AuditoriaService auditoriaService) {
        this.reservaService = reservaService;
        this.docenteResolver = docenteResolver != null ? docenteResolver : new DocenteResolver();
        this.auditoriaService = auditoriaService;
    }


    @PostMapping
    public ResponseEntity<ReservaResponse> crearSolicitud(
            @Valid @RequestBody CrearReservaRequest request,
            Authentication authentication
    ) {
        if (authentication != null && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DOCENTE"))) {
            Long docenteIdAutenticado = docenteResolver.resolverDocenteId(authentication.getName());
            request.setDocenteId(docenteIdAutenticado);
        }
        ReservaResponse response = reservaService.crearSolicitud(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ReservaResponse>> listarTodas(
            @RequestParam(required = false) Long docenteId,
            Authentication authentication
    ) {
        if (authentication != null && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DOCENTE"))) {
            boolean esBibliotecaria = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_BIBLIOTECARIA"));
            if (!esBibliotecaria) {
                Long miDocenteId = docenteResolver.resolverDocenteId(authentication.getName());
                if (docenteId != null && !docenteId.equals(miDocenteId)) {
                    throw new AccessDeniedException("No tiene permisos para consultar solicitudes de otro docente.");
                }
                return ResponseEntity.ok(reservaService.listarMisSolicitudes(miDocenteId));
            }
        }
        if (docenteId != null) {
            return ResponseEntity.ok(reservaService.listarMisSolicitudes(docenteId));
        }
        return ResponseEntity.ok(reservaService.listarTodas());
    }

    @GetMapping("/mis-solicitudes")
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<List<ReservaResponse>> listarMisSolicitudes(Authentication authentication) {
        Long docenteId = docenteResolver.resolverDocenteId(authentication.getName());
        return ResponseEntity.ok(reservaService.listarMisSolicitudes(docenteId));
    }

    @GetMapping("/confirmadas")
    public ResponseEntity<List<ReservaResponse>> listarConfirmadas(
            @RequestParam(required = false) Long equipoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        return ResponseEntity.ok(reservaService.listarConfirmadas(equipoId, fecha));
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

    @GetMapping("/{id}/auditoria")
    @PreAuthorize("hasRole('BIBLIOTECARIA')")
    public ResponseEntity<List<AuditoriaResponse>> obtenerAuditoria(@PathVariable Long id) {
        if (auditoriaService == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(auditoriaService.obtenerPorReservaId(id));
    }
}

