package com.biblioteca.horizonte.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resuelve el identificador de dominio (docenteId) a partir de la identidad autenticada
 * del usuario en Spring Security.
 *
 * Mapeo soportado:
 *  - "docente"   -> 1L (usuario por defecto para MVP y compatibilidad hacia atrás)
 *  - "docente1"  -> 1L (Prof. Juan Pérez)
 *  - "docente2"  -> 2L (Prof. María González)
 *  - "1", "2"    -> 1L, 2L (identificador directo como username)
 *  - Cualquier nombre que finalice en dígitos -> extrae dicho ID numérico.
 */
@Component
public class DocenteResolver {

    private static final Pattern TRAILING_DIGITS = Pattern.compile("(\\d+)$");

    public Long resolverDocenteId(String username) {
        if (username == null || username.isBlank()) {
            throw new AccessDeniedException("No se encontró usuario autenticado.");
        }

        if ("docente".equalsIgnoreCase(username)) {
            return 1L;
        }

        Matcher matcher = TRAILING_DIGITS.matcher(username);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }

        // Valor por defecto para compatibilidad
        return 1L;
    }
}
