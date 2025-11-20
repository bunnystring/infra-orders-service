package com.infragest.infra_orders_service.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Jwts;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

/**
 * Utilidad para operaciones comunes con tokens JWT.
 *
 * Proporciona inicialización de la clave secreta a partir de una cadena Base64,
 * extracción del email (subject) desde el token y validación de la integridad y
 * validez del JWT.
 *
 * @author bunnystring
 * @since 2025-11-19
 */
@Component
public class JwtUtil {

    /**
     * Clave del JWT codificada en Base64. Se inyecta desde la propiedad:
     * {@code spring.security.oauth2.resourceserver.jwt.secret}.
     */
    @Value("${spring.security.oauth2.resourceserver.jwt.secret}")
    private String jwtSecretBase64;

    /**
     * Representación en bytes de la clave secreta (HMAC SHA-256) construida en {@link #init()}.
     */
    private Key secretKey;

    /**
     * Inicializa la clave secreta decodificando {@link #jwtSecretBase64} (Base64) y
     * construyendo una {@link SecretKeySpec} para uso en la verificación de firmas HMAC-SHA256.
     *
     * @throws IllegalArgumentException si la propiedad {@code jwtSecretBase64} no es una cadena Base64 válida
     *                                  o si la clave resultante no es adecuada para HmacSHA256
     */
    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecretBase64);
        this.secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    /**
     * Extrae el email (subject) de un token JWT válido.
     *
     * @param token JWT en formato compact (ej. "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
     * @return el valor del claim "sub" (subject) que en este proyecto representa el email del usuario
     * @throws JwtException si el token es inválido, está mal formado o la firma no coincide
     * @throws IllegalArgumentException si {@code token} es {@code null} o vacío
     */
    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Valida la integridad y validez de un token JWT comprobando firma y estructura.
     *
     * @param token JWT a validar
     * @return {@code true} si el token es válido; {@code false} si es inválido, está expirado
     *         o ocurre cualquier error de parseo/validación
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
