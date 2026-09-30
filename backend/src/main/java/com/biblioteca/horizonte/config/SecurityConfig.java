package com.biblioteca.horizonte.config;

import com.biblioteca.horizonte.security.RestAccessDeniedHandler;
import com.biblioteca.horizonte.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración de seguridad para el MVP de Biblioteca Horizonte.
 *
 * Autenticación: HTTP Basic (adecuado para MVP académico).
 * Sesión: STATELESS — cada request debe enviar credenciales.
 *
 * Roles definidos:
 *   - ROLE_DOCENTE     → puede crear solicitudes de reserva
 *   - ROLE_BIBLIOTECARIA → puede confirmar y rechazar solicitudes
 *
 * Credenciales de desarrollo:
 *   docente       / docente123   → ROLE_DOCENTE
 *   bibliotecaria / biblio123    → ROLE_BIBLIOTECARIA
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(RestAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS manejado por CorsConfigurationSource (ver corsConfigurationSource())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Deshabilitar CSRF: API REST stateless con HTTP Basic no requiere CSRF
            .csrf(csrf -> csrf.disable())
            // Sesión STATELESS: no se crean sesiones HTTP
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Handlers de error personalizados para retornar JSON en 401/403
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                // Salud pública — no requiere autenticación
                .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                // H2 console en desarrollo (permitida sin auth)
                .requestMatchers("/h2-console/**").permitAll()
                // Listar equipos es público (el docente necesita ver la lista para hacer una solicitud)
                .requestMatchers(HttpMethod.GET, "/api/v1/equipos").permitAll()
                // PROTEGIDO: confirmar y rechazar solo BIBLIOTECARIA
                .requestMatchers(HttpMethod.POST, "/api/v1/reservas/*/confirmar").hasRole("BIBLIOTECARIA")
                .requestMatchers(HttpMethod.POST, "/api/v1/reservas/*/rechazar").hasRole("BIBLIOTECARIA")
                // Mis solicitudes solo para DOCENTE
                .requestMatchers(HttpMethod.GET, "/api/v1/reservas/mis-solicitudes").hasRole("DOCENTE")
                // Crear una solicitud requiere estar autenticado
                .requestMatchers(HttpMethod.POST, "/api/v1/reservas").authenticated()
                // Listar/obtener reservas requiere autenticación
                .requestMatchers(HttpMethod.GET, "/api/v1/reservas", "/api/v1/reservas/**").authenticated()
                // Cualquier otro endpoint requiere autenticación
                .anyRequest().authenticated()
            )
            // HTTP Basic para el MVP académico
            .httpBasic(Customizer.withDefaults());

        // Necesario para que la consola H2 funcione dentro de iframes
        http.headers(headers -> headers.frameOptions(fo -> fo.sameOrigin()));

        return http.build();
    }

    /**
     * Usuarios en memoria para desarrollo/MVP.
     * IMPORTANTE: En producción reemplazar por UserDetailsService basado en base de datos.
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        var docente = User.builder()
                .username("docente")
                .password(passwordEncoder.encode("docente123"))
                .roles("DOCENTE")
                .build();

        var docente1 = User.builder()
                .username("docente1")
                .password(passwordEncoder.encode("docente123"))
                .roles("DOCENTE")
                .build();

        var docente2 = User.builder()
                .username("docente2")
                .password(passwordEncoder.encode("docente123"))
                .roles("DOCENTE")
                .build();

        var bibliotecaria = User.builder()
                .username("bibliotecaria")
                .password(passwordEncoder.encode("biblio123"))
                .roles("BIBLIOTECARIA")
                .build();

        return new InMemoryUserDetailsManager(docente, docente1, docente2, bibliotecaria);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Fuente de configuración CORS compatible con Spring Security.
     * Reemplaza el WebMvcConfigurer de CorsConfig para que Spring Security
     * aplique las políticas CORS antes de evaluar la autenticación.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://127.0.0.1:5173"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
