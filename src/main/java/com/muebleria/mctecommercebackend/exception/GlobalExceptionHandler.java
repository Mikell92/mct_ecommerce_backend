package com.muebleria.mctecommercebackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException; // Importar esta
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest; // Posiblemente necesaria para contexto
import org.springframework.web.servlet.NoHandlerFoundException; // Para 404 de rutas no mapeadas


import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para la aplicación.
 * Captura excepciones lanzadas por los controladores y servicios
 * y las convierte en respuestas HTTP uniformes.
 */
@RestControllerAdvice // Indica que esta clase maneja excepciones de todos los controladores
public class GlobalExceptionHandler {

    /**
     * Maneja las excepciones de validación de argumentos (@Valid).
     * Retorna un mapa con los nombres de los campos y sus mensajes de error.
     * Corresponde a un 400 Bad Request.
     *
     * @param ex La excepción {@link MethodArgumentNotValidException}.
     * @return Un mapa de errores con detalles de validación.
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((org.springframework.validation.FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }

    /**
     * Maneja las excepciones de recurso no encontrado (ResourceNotFoundException).
     * Corresponde a un 404 Not Found.
     *
     * @param ex La excepción {@link ResourceNotFoundException}.
     * @return Un mapa que contiene el mensaje de error.
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceNotFoundException.class)
    public Map<String, String> handleNotFoundException(ResourceNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return error;
    }

    /**
     * Maneja excepciones de negocio generales (RuntimeException),
     * como nombres duplicados.
     * Corresponde a un 409 Conflict.
     *
     * @param ex La excepción {@link RuntimeException}.
     * @return Un mapa que contiene el mensaje de error.
     */
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(RuntimeException.class)
    public Map<String, String> handleBusinessExceptions(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return error;
    }

    /**
     * Maneja las excepciones de acceso denegado (AccessDeniedException).
     * Esto ocurre cuando un usuario autenticado intenta acceder a un recurso
     * para el cual no tiene los permisos necesarios (ej. rol incorrecto).
     * Retorna un 403 Forbidden.
     *
     * @param ex La excepción {@link AccessDeniedException}.
     * @param request La solicitud web actual.
     * @return Una respuesta de error con estado 403.
     */
    @ResponseStatus(HttpStatus.FORBIDDEN) // Establece el código de estado HTTP a 403 Forbidden
    @ExceptionHandler(AccessDeniedException.class)
    public Map<String, String> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Acceso Denegado");
        error.put("message", ex.getMessage());
        error.put("path", request.getDescription(false).replace("uri=", "")); // Elimina "uri="
        return error;
    }

    /**
     * Manejador de excepciones genérico para cualquier otra excepción no capturada.
     * Sirve como un fallback para evitar que Spring Boot use su página de error por defecto.
     * Retorna un 500 Internal Server Error.
     *
     * @param ex La excepción {@link Exception} genérica.
     * @return Un mapa que contiene un mensaje de error genérico.
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // Establece el código de estado HTTP a 500
    @ExceptionHandler(Exception.class)
    public Map<String, String> handleAllUncaughtException(Exception ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Error Interno del Servidor");
        error.put("message", "Ha ocurrido un error inesperado. Por favor, intente de nuevo más tarde.");
        // Opcional: Para depuración, podrías incluir ex.getMessage() aquí en desarrollo,
        // pero quítalo en producción por seguridad.
        return error;
    }
}