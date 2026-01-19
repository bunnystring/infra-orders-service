package com.infragest.infra_orders_service.config;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

@Slf4j
@Configuration
public class FeignClientConfig {

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

    private String getJwtToken() {
        Object authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof BearerTokenAuthentication bearerToken) {
            return bearerToken.getToken().getTokenValue();
        }

        throw new IllegalStateException("No se encontró un token en el contexto de seguridad.");
    }
}
