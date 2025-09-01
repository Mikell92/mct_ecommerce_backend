package com.muebleria.mctecommercebackend.service.impl;

import com.muebleria.mctecommercebackend.dto.UserAccessRuleDTO;
import com.muebleria.mctecommercebackend.exception.ResourceNotFoundException;
import com.muebleria.mctecommercebackend.model.Role;
import com.muebleria.mctecommercebackend.model.User;
import com.muebleria.mctecommercebackend.model.UserAccessRule;
import com.muebleria.mctecommercebackend.repository.UserAccessRuleRepository;
import com.muebleria.mctecommercebackend.repository.UserRepository;
import com.muebleria.mctecommercebackend.security.user.UserDetailsImpl;
import com.muebleria.mctecommercebackend.service.UserAccessRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserAccessRuleServiceImpl implements UserAccessRuleService {

    private final UserAccessRuleRepository ruleRepository;
    private final UserRepository userRepository;

    @Autowired
    public UserAccessRuleServiceImpl(UserAccessRuleRepository ruleRepository, UserRepository userRepository) {
        this.ruleRepository = ruleRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserAccessRuleDTO createRuleForUser(Long userId, UserAccessRuleDTO ruleDTO) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        if (targetUser.isBypassAccessRules()) {
            throw new IllegalArgumentException("No se pueden asignar horarios a este usuario porque tiene permiso para omitir las reglas de acceso.");
        }
        if (!canManageSchedules(currentUser, targetUser)) {
            throw new AccessDeniedException("No tienes permiso para gestionar los horarios de este usuario.");
        }

        DayOfWeek day = DayOfWeek.valueOf(ruleDTO.getDayOfWeek());
        ruleRepository.findByUserIdAndDayOfWeek(userId, day).ifPresent(r -> {
            throw new RuntimeException("Ya existe una regla para el usuario " + userId + " en el día " + day);
        });

        UserAccessRule rule = new UserAccessRule();
        mapDtoToEntity(ruleDTO, rule);
        rule.setUser(targetUser);
        rule.setCreatedBy(currentUser);
        rule.setUpdatedBy(currentUser);

        UserAccessRule savedRule = ruleRepository.save(rule);
        return toDTO(savedRule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAccessRuleDTO> getRulesByUserId(Long userId) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));
        // Usamos findById, que ahora puede encontrar usuarios eliminados, permitiendo la auditoría.
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        if (!canManageSchedules(currentUser, targetUser)) {
            throw new AccessDeniedException("No tienes permiso para ver los horarios de este usuario.");
        }

        return ruleRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserAccessRuleDTO updateRule(Long ruleId, UserAccessRuleDTO ruleDTO) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));
        UserAccessRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Regla no encontrada con ID: " + ruleId));

        if (!canManageSchedules(currentUser, rule.getUser())) {
            throw new AccessDeniedException("No tienes permiso para gestionar los horarios de este usuario.");
        }

        DayOfWeek newDay = DayOfWeek.valueOf(ruleDTO.getDayOfWeek());
        if (newDay != rule.getDayOfWeek()) {
            ruleRepository.findByUserIdAndDayOfWeek(rule.getUser().getId(), newDay).ifPresent(r -> {
                throw new RuntimeException("El usuario ya tiene una regla para el día " + newDay);
            });
        }

        mapDtoToEntity(ruleDTO, rule);
        rule.setUpdatedBy(currentUser);

        UserAccessRule updatedRule = ruleRepository.save(rule);
        return toDTO(updatedRule);
    }

    @Override
    @Transactional
    public void deleteRule(Long ruleId) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));
        UserAccessRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Regla no encontrada con ID: " + ruleId));

        if (!canManageSchedules(currentUser, rule.getUser())) {
            throw new AccessDeniedException("No tienes permiso para gestionar los horarios de este usuario.");
        }

        ruleRepository.deleteById(ruleId);
    }

    private boolean canManageSchedules(User currentUser, User targetUser) {
        if (currentUser.getId().equals(targetUser.getId())) {
            return false;
        }
        Role currentUserRole = currentUser.getRole();
        Role targetUserRole = targetUser.getRole();
        if (currentUserRole == Role.DEVELOPER) {
            return targetUserRole != Role.DEVELOPER;
        }
        if (currentUserRole == Role.ADMIN) {
            return targetUserRole.getLevel() < currentUserRole.getLevel();
        }
        return false;
    }

    private Optional<User> getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return Optional.empty();
        }
        Long currentUserId = ((UserDetailsImpl) authentication.getPrincipal()).getId();
        // Usamos el findById que puede encontrar usuarios eliminados, para asegurar que la sesión es válida.
        return userRepository.findById(currentUserId);
    }

    private UserAccessRuleDTO toDTO(UserAccessRule rule) {
        return new UserAccessRuleDTO(
                rule.getId(),
                rule.getUser().getId(),
                rule.getDayOfWeek().toString(),
                rule.getStartTime(),
                rule.getEndTime(),
                rule.getAccessTimezone(),
                rule.isActive()
        );
    }

    private void mapDtoToEntity(UserAccessRuleDTO dto, UserAccessRule entity) {
        entity.setDayOfWeek(DayOfWeek.valueOf(dto.getDayOfWeek()));
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setAccessTimezone(dto.getAccessTimezone());
        entity.setActive(dto.isActive());
    }
}