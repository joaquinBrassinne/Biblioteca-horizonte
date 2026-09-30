package com.biblioteca.horizonte.repository;

import com.biblioteca.horizonte.entity.AuditoriaReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditoriaReservaRepository extends JpaRepository<AuditoriaReserva, Long> {

    List<AuditoriaReserva> findByReservaId(Long reservaId);

    List<AuditoriaReserva> findByUsuario(String usuario);

    List<AuditoriaReserva> findByOperacion(String operacion);
}
