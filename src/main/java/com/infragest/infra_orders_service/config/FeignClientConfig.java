package com.infragest.infra_orders_service.config;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

/**
 * Configuración de Feign Client para agregar autenticación mediante un Bearer Token en las solicitudes salientes.
 *
 * @author bunnystring
 * @since 2026-01-27
 */
@Slf4j
@Configuration
public class FeignClientConfig {

    /**
     * Define un interceptor de solicitudes para Feign Client y adjunta el token JWT en el encabezado "Authorization".
     * <p>
     * Este interceptor utiliza el contexto de seguridad actual para recuperar las credenciales (Bearer Token)
     * y las incluye automáticamente en las solicitudes API salientes. Si no se encuentra un token,
     * se genera un log de advertencia y se arroja una excepción.
     * </p>
     *
     * @return un {@code RequestInterceptor} que intercepta las solicitudes y agrega el encabezado de autorización.
     * @throws IllegalStateException si no hay un token presente en el contexto de seguridad de Spring.
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            Object authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication instanceof Authentication auth) {
                if (auth.getCredentials() != null) {
                    String token = auth.getCredentials().toString();
                    requestTemplate.header("Authorization", "Bearer " + token);
                }
            } else {
                log.warn("No se encontró un token en el contexto de seguridad para Feign.");
                throw new IllegalStateException("No se encontró un token en el contexto de seguridad.");
            }
        };
    }

    /**
     * Recupera el Bearer Token (JWT) del contexto de seguridad de Spring.
     * <p>
     * Este método busca el token en una instancia de {@link BearerTokenAuthentication}.
     * Si no se encuentra un token válido, lanza una excepción para indicar el problema.
     * </p>
     *
     * @return el Bearer Token (JWT) como {@link String}.
     * @throws IllegalStateException si no se encuentra un token válido en el contexto de seguridad.
     */
    private String getJwtToken() {
        Object authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof BearerTokenAuthentication bearerToken) {
            return bearerToken.getToken().getTokenValue();
        }

        throw new IllegalStateException("No se encontró un token en el contexto de seguridad.");
    }
}
