package com.infragest.infra_orders_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filtro de seguridad que se ejecuta una vez por petición y que se encarga de:
 *
 * Esta implementación es deliberadamente sencilla: no carga detalles del usuario
 * desde DB ni asigna roles (usa {@link Collections#emptyList()} para los authorities).
 *
 * @author bunnystring
 * @since 2025-11-19
 */
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    /**
     * Utilidad para parsear y validar tokens JWT.
     */
    private final JwtUtil jwtUtil;

    /**
     * Constructor para inyección de dependencias.
     *
     * @param jwtUtil utilidad encargada de la validación y extracción de claims del JWT
     */
    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Implementación del filtro que:
     *
     * @param request  petición HTTP entrante
     * @param response respuesta HTTP
     * @param filterChain cadena de filtros restante
     * @throws ServletException si ocurre un error a nivel de servlet durante el filtrado
     * @throws IOException si ocurre un error de E/S al escribir la respuesta
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.info("Token procesado: {}", token);

            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.getEmailFromToken(token);
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        email,
                        token,
                        Collections.emptyList()
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("Usuario autenticado: {}", email);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token inválido o expirado.");
                return;
            }
        } else {
            // Si falta el header Authorization o no es Bearer, rechaza la petición
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Authorization header ausente o mal formado.");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
