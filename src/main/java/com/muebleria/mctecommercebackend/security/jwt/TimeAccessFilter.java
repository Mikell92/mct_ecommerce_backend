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

        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetailsImpl) || request.getRequestURI().startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        // Usamos el nuevo método del repositorio que carga las reglas de acceso de forma anticipada
        Optional<User> userOpt = userRepository.findByIdWithAccessRules(userId);

        if (userOpt.isEmpty()) {
            sendErrorResponse(response, "Usuario no encontrado.");
            return;
        }

        User user = userOpt.get();

        if (user.isBypassAccessRules()) {
            filterChain.doFilter(request, response);
            return;
        }

        for (UserAccessRule rule : user.getAccessRules()) {
            if (rule.isActive() && isTimeAllowed(rule)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        logger.warn("Acceso denegado para el usuario '{}' fuera de su horario permitido.", user.getUsername());
        sendErrorResponse(response, "Acceso fuera del horario permitido.");
    }

    private boolean isTimeAllowed(UserAccessRule rule) {
        try {
            ZoneId ruleZoneId = ZoneId.of(rule.getAccessTimezone());
            ZonedDateTime nowInRuleZone = ZonedDateTime.now(ruleZoneId);
            DayOfWeek currentDayInZone = nowInRuleZone.getDayOfWeek();
            LocalTime currentTimeInZone = nowInRuleZone.toLocalTime();

            if (rule.getDayOfWeek() == currentDayInZone) {
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