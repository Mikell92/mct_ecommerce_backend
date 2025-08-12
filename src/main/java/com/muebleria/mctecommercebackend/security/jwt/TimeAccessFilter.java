package com.muebleria.mctecommercebackend.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muebleria.mctecommercebackend.model.User;
import com.muebleria.mctecommercebackend.model.UserAccessRule;
import com.muebleria.mctecommercebackend.repository.UserRepository;
import com.muebleria.mctecommercebackend.security.user.UserDetailsImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class TimeAccessFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TimeAccessFilter.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 1. Si no hay un usuario autenticado o la ruta es de login, no hacemos nada.
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetailsImpl) || request.getRequestURI().startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Obtenemos el ID del usuario actual.
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        // 3. Buscamos el usuario y sus reglas en la base de datos.
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            // Esto sería raro, pero por seguridad, denegamos el acceso.
            sendErrorResponse(response, "Usuario no encontrado.");
            return;
        }

        User user = userOpt.get();

        // 4. Si el usuario puede omitir las reglas, le damos acceso inmediato.
        if (user.isBypassAccessRules()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 5. Verificamos si hay una regla activa para el día y hora actual.
        for (UserAccessRule rule : user.getAccessRules()) {
            if (rule.isActive() && isTimeAllowed(rule)) {
                // Si encontramos una regla válida, permitimos el acceso y terminamos.
                filterChain.doFilter(request, response);
                return;
            }
        }

        // 6. Si el bucle termina y no se encontró una regla válida, denegamos el acceso.
        logger.warn("Acceso denegado para el usuario '{}' fuera de su horario permitido.", user.getUsername());
        sendErrorResponse(response, "Acceso fuera del horario permitido.");
    }

    private boolean isTimeAllowed(UserAccessRule rule) {
        try {
            ZoneId ruleZoneId = ZoneId.of(rule.getAccessTimezone());
            ZonedDateTime nowInRuleZone = ZonedDateTime.now(ruleZoneId);
            DayOfWeek currentDayInZone = nowInRuleZone.getDayOfWeek();
            LocalTime currentTimeInZone = nowInRuleZone.toLocalTime();

            // Comprueba si la regla es para el día de hoy
            if (rule.getDayOfWeek() == currentDayInZone) {
                // Comprueba si la hora actual está dentro del rango permitido
                return !currentTimeInZone.isBefore(rule.getStartTime()) && !currentTimeInZone.isAfter(rule.getEndTime());
            }
        } catch (Exception e) {
            logger.error("Error al procesar la regla de acceso ID {}: {}", rule.getId(), e.getMessage());
        }
        return false;
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        Map<String, String> error = new HashMap<>();
        error.put("error", "Acceso Denegado");
        error.put("message", message);
        response.getWriter().write(new ObjectMapper().writeValueAsString(error));
    }
}