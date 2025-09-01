package com.muebleria.mctecommercebackend.security.jwt;

import com.muebleria.mctecommercebackend.security.user.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

// Clase de utilidad para JWT: Genera, parsea y valida tokens.
@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    // Clave secreta para firmar los JWT. Se inyecta desde application.properties.
    @Value("${muebleria.app.jwtSecret}")
    private String jwtSecret;

    // Tiempo de expiración del JWT en milisegundos. Se inyecta desde application.properties.
    @Value("${muebleria.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    // Obtiene la clave de firma a partir del secreto JWT.
    private Key key() {
        // Trata el secreto como una cadena de texto UTF-8 y la convierte a bytes.
        // Esta es la forma correcta y estándar de manejar claves de texto plano.
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // Genera un JWT a partir de la autenticación del usuario.
    public String generateJwtToken(Authentication authentication) {
        // Obtiene los detalles del usuario autenticado.
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        // Construye el token JWT con el nombre de usuario, fechas de emisión y expiración, y la firma.
        return Jwts.builder()
                .setSubject(userPrincipal.getUsername()) // El sujeto del token es el nombre de usuario
                .setIssuedAt(new Date()) // Fecha de emisión del token
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs)) // Fecha de expiración
                .signWith(key(), SignatureAlgorithm.HS256) // Firma el token con la clave secreta y el algoritmo HS256
                .compact(); // Construye el JWT
    }

    // Obtiene el nombre de usuario del token JWT.
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().getSubject(); // Extrae el sujeto (nombre de usuario)
    }

    public Claims getClaimsFromJwtToken(String token) {
        return Jwts.parser().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody();
    }

    // Valida un token JWT.
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(key()).build().parse(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Token JWT inválido: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("Token JWT expirado: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("Token JWT no soportado: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("Cadena de claims JWT vacía: {}", e.getMessage());
        }
        return false;
    }
}