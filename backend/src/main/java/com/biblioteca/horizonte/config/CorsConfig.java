package com.biblioteca.horizonte.config;

/**
 * La configuración CORS está definida en {@link SecurityConfig#corsConfigurationSource()}.
 *
 * Cuando Spring Security está presente, la configuración CORS debe registrarse a través
 * del {@code CorsConfigurationSource} bean para que Spring Security la aplique ANTES
 * de evaluar la autenticación. Registrar CORS solo en WebMvcConfigurer no es suficiente
 * porque Spring Security intercepta la request primero.
 *
 * Esta clase se conserva como referencia, pero no está activa.
 */
public class CorsConfig {
    // Ver SecurityConfig.corsConfigurationSource()
}
