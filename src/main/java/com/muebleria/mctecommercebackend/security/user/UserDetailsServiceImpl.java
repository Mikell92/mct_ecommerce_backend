package com.muebleria.mctecommercebackend.security.user;

import com.muebleria.mctecommercebackend.model.User;
import com.muebleria.mctecommercebackend.model.UserAccessRule;
import com.muebleria.mctecommercebackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con nombre: " + username));

        // Verificación de Horario en el Login
        // Si el usuario NO puede omitir las reglas, verificamos su horario.
        if (!user.isBypassAccessRules()) {
            boolean isAllowed = false;
            // Si no tiene reglas asignadas, no puede entrar.
            if (user.getAccessRules() == null || user.getAccessRules().isEmpty()) {
                throw new LockedException("Acceso denegado: No tienes un horario de trabajo asignado.");
            }

            // Iteramos sobre sus reglas para ver si alguna permite el acceso ahora mismo.
            for (UserAccessRule rule : user.getAccessRules()) {
                if (rule.isActive() && isTimeAllowed(rule)) {
                    isAllowed = true;
                    break; // Si encontramos una regla válida, rompemos el bucle.
                }
            }

            // Si después de revisar todas las reglas, ninguna permitió el acceso...
            if (!isAllowed) {
                // Lanzamos una excepción que Spring Security interpreta como "cuenta bloqueada".
                throw new LockedException("Acceso denegado: Estás fuera de tu horario de trabajo permitido.");
            }
        }

        return UserDetailsImpl.build(user);
    }

    /**
     * Lógica auxiliar para verificar si la hora actual está dentro del rango de una regla.
     * Extraída de TimeAccessFilter para reutilización.
     */
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
            // Log del error sería apropiado aquí en un entorno real.
            return false;
        }
        return false;
    }
}