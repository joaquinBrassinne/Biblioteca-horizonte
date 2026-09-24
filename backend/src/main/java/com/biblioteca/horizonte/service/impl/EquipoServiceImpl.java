package com.biblioteca.horizonte.service.impl;

import com.biblioteca.horizonte.dto.response.EquipoResponse;
import com.biblioteca.horizonte.repository.EquipoRepository;
import com.biblioteca.horizonte.service.EquipoService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EquipoServiceImpl implements EquipoService {

    private final EquipoRepository equipoRepository;

    public EquipoServiceImpl(EquipoRepository equipoRepository) {
        this.equipoRepository = equipoRepository;
    }

    @Override
    public List<EquipoResponse> listarEquipos() {
        return equipoRepository.findAll().stream()
                .map(e -> new EquipoResponse(e.getId(), e.getNombre()))
                .toList();
    }
}
