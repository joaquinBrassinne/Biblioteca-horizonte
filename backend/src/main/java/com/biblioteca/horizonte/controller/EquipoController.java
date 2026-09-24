package com.biblioteca.horizonte.controller;

import com.biblioteca.horizonte.dto.response.EquipoResponse;
import com.biblioteca.horizonte.service.EquipoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/equipos")
public class EquipoController {

    private final EquipoService equipoService;

    public EquipoController(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    @GetMapping
    public ResponseEntity<List<EquipoResponse>> listarEquipos() {
        return ResponseEntity.ok(equipoService.listarEquipos());
    }
}
