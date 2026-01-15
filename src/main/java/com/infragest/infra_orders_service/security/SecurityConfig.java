package com.infragest.infra_orders_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de seguridad para el servicio de grupos.
 *
 * @author bunnystring
 * @since 2025-11-19
 */
@Configuration
public class SecurityConfig   {

    /**
     * Filtro responsable de extraer y validar el JWT (Authorization: Bearer &lt;token&gt;)
     * y de poblar el SecurityContext con la {@code Authentication} correspondiente.
     */
    private final JwtAuthFilter jwtAuthFilter;

    /**
     * Constructor para la inyección de dependencias.
     *
     * @param jwtAuthFilter filtro que valida tokens JWT y construye la autenticación
     */
    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    /**
     * Construye y configura la cadena de filtros de seguridad.
     *
     * @param http objeto {@link HttpSecurity} proporcionado por Spring Security
     * @return la {@link SecurityFilterChain} construida
     * @throws Exception si ocurre algún error al construir la configuración de seguridad
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()   // TODAS las rutas deben ir autenticadas por token
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
