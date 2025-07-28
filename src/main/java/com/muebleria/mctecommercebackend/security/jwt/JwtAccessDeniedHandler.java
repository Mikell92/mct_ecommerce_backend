package com.muebleria.mctecommercebackend.security.jwt;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException; // Importa AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler; // Importa AccessDeniedHandler
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Manejador personalizado para denegaciones de acceso (errores 403 Forbidden).
 * Se invoca cuando un usuario autenticado intenta acceder a un recurso
 * para el cual no tiene los permisos necesarios (ej. por rol).
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger logger = LoggerFactory.getLogger(JwtAccessDeniedHandler.class);

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        logger.error("Error de acceso denegado: {}", accessDeniedException.getMessage());
        // Envía una respuesta 403 Forbidden
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Error: Acceso Denegado. No tienes permiso para este recurso.");
    }
}