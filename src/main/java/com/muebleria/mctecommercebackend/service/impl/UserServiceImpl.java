package com.muebleria.mctecommercebackend.service.impl;

import com.muebleria.mctecommercebackend.dto.*;
import com.muebleria.mctecommercebackend.exception.ResourceNotFoundException;
import com.muebleria.mctecommercebackend.model.*;
import com.muebleria.mctecommercebackend.repository.*;
import com.muebleria.mctecommercebackend.security.user.UserDetailsImpl;
import com.muebleria.mctecommercebackend.service.UserService;
import jakarta.persistence.criteria.Join;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAccessRuleRepository ruleRepository;
    private final UserProfileRepository userProfileRepository;
    private final DriverDetailRepository driverDetailRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, BranchRepository branchRepository, PasswordEncoder passwordEncoder, UserAccessRuleRepository ruleRepository, UserProfileRepository userProfileRepository, DriverDetailRepository driverDetailRepository) {
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
        this.ruleRepository = ruleRepository;
        this.userProfileRepository = userProfileRepository;
        this.driverDetailRepository = driverDetailRepository;
    }

    @Override
    @Transactional
    public UserDTO createUser(UserDTO userDTO) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));
        Role roleToCreate = Role.fromString(userDTO.getRole());
        if (roleToCreate == null) throw new IllegalArgumentException("El rol proporcionado no es válido.");

        if (!canCreate(currentUser.getRole(), roleToCreate)) {
            throw new AccessDeniedException("No tienes permiso para crear usuarios con el rol " + roleToCreate.name());
        }
        if (userDTO.isBypassAccessRules() && userDTO.getAccessRules() != null && !userDTO.getAccessRules().isEmpty()) {
            throw new IllegalArgumentException("No se pueden asignar horarios a un usuario que tiene permiso para omitir las reglas de acceso.");
        }
        userRepository.findByUsername(userDTO.getUsername()).ifPresent(u -> {
            throw new RuntimeException("El nombre de usuario '" + userDTO.getUsername() + "' ya existe.");
        });

        User user = new User();
        mapBaseUserDtoToEntity(userDTO, user);
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setCreatedBy(currentUser);
        user.setUpdatedBy(currentUser);
        User savedUser = userRepository.save(user);

        if (userDTO.getProfile() == null) {
            throw new IllegalArgumentException("La información del perfil (nombre y apellido) es obligatoria.");
        }

        UserProfile profile = new UserProfile();
        profile.setUser(savedUser);
        mapProfileDtoToEntity(userDTO.getProfile(), profile);
        profile.setCreatedBy(currentUser);
        profile.setUpdatedBy(currentUser);
        userProfileRepository.save(profile);

        savedUser.setProfile(profile);

        if (roleToCreate == Role.DRIVER) {
            if (userDTO.getDriverDetails() == null || userDTO.getDriverDetails().getLicenseNumber() == null) {
                throw new IllegalArgumentException("El número de licencia es obligatorio para usuarios con rol DRIVER.");
            }
            DriverDetail driverDetail = new DriverDetail();
            driverDetail.setUser(savedUser);
            mapDriverDetailDtoToEntity(userDTO.getDriverDetails(), driverDetail);
            driverDetail.setCreatedBy(currentUser);
            driverDetail.setUpdatedBy(currentUser);
            driverDetailRepository.save(driverDetail);
            savedUser.setDriverDetail(driverDetail);
        }

        if (userDTO.getAccessRules() != null && !userDTO.getAccessRules().isEmpty()) {
            for (UserAccessRuleDTO ruleDTO : userDTO.getAccessRules()) {
                UserAccessRule rule = new UserAccessRule();
                rule.setUser(savedUser);
                rule.setDayOfWeek(DayOfWeek.valueOf(ruleDTO.getDayOfWeek()));
                rule.setStartTime(ruleDTO.getStartTime());
                rule.setEndTime(ruleDTO.getEndTime());
                rule.setAccessTimezone(ruleDTO.getAccessTimezone());
                rule.setActive(ruleDTO.isActive());
                rule.setCreatedBy(currentUser);
                rule.setUpdatedBy(currentUser);
                ruleRepository.save(rule);
                savedUser.getAccessRules().add(rule);
            }
        }

        return toDTO(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfileViewDTO> findMyProfileById(Long id) {
        return userRepository.findById(id).map(this::toUserProfileViewDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDTO> findById(Long id) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        if (targetUser.isDeleted()) {
            if (currentUser.getRole() != Role.DEVELOPER) {
                throw new ResourceNotFoundException("Usuario no encontrado con ID: " + id);
            }
        }

        if (!canRead(currentUser, targetUser)) {
            throw new AccessDeniedException("No tienes permiso para ver este usuario.");
        }

        return Optional.of(toDTO(targetUser));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameTaken(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDTO> findByUsername(String username) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));
        Optional<User> targetUserOpt = userRepository.findByUsername(username);

        targetUserOpt.ifPresent(targetUser -> {
            if (!canRead(currentUser, targetUser)) {
                throw new AccessDeniedException("No tienes permiso para ver este usuario.");
            }
        });

        return targetUserOpt.map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryDTO> findAll(Pageable pageable, UserStatus status, String search, String role, Long branchId) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));

        // Comprobación de seguridad (sin cambios)
        if (status == UserStatus.DELETED && currentUser.getRole() != Role.DEVELOPER) {
            throw new AccessDeniedException("No tienes permiso para ver la lista de usuarios eliminados.");
        }

        // --- INICIO DE LA LÓGICA UNIFICADA ---

        Specification<User> spec = (root, query, cb) -> cb.conjunction();

        // Filtro para no incluir al usuario que hace la consulta
        spec = spec.and((root, query, cb) -> cb.notEqual(root.get("id"), currentUser.getId()));

        // Filtro de jerarquía de roles (sin cambios)
        Role currentUserRole = currentUser.getRole();
        if (currentUserRole == Role.DEVELOPER) {
            spec = spec.and((root, query, cb) -> cb.notEqual(root.get("role"), Role.DEVELOPER));
        } else if (currentUserRole == Role.ADMIN) {
            spec = spec.and((root, query, cb) -> cb.notEqual(root.get("role"), Role.DEVELOPER));
            spec = spec.and((root, query, cb) -> cb.notEqual(root.get("role"), Role.ADMIN));
        }

        // Filtro de estado (Status) - Ahora controla si se ven los eliminados o no
        UserStatus finalStatus = (status == null) ? UserStatus.ACTIVE : status;

        switch (finalStatus) {
            case DELETED:
                spec = spec.and((root, query, cb) -> cb.isTrue(root.get("isDeleted")));
                break;
            case ACTIVE:
                spec = spec.and((root, query, cb) -> cb.isFalse(root.get("isDeleted")));
                spec = spec.and((root, query, cb) -> cb.isTrue(root.get("active")));
                break;
            case INACTIVE:
                spec = spec.and((root, query, cb) -> cb.isFalse(root.get("isDeleted")));
                spec = spec.and((root, query, cb) -> cb.isFalse(root.get("active")));
                break;
            case ALL:
                spec = spec.and((root, query, cb) -> cb.isFalse(root.get("isDeleted")));
                break;
        }

        // A PARTIR DE AQUÍ, LOS FILTROS DE ROL Y BÚSQUEDA SE APLICAN A TODOS LOS CASOS

        if (search != null && !search.trim().isEmpty()) {
            String searchTerm = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> {
                Join<User, UserProfile> profileJoin = root.join("profile");
                return cb.or(
                        cb.like(cb.lower(root.get("username")), searchTerm),
                        cb.like(cb.lower(profileJoin.get("firstName")), searchTerm),
                        cb.like(cb.lower(profileJoin.get("lastName")), searchTerm),
                        cb.like(cb.lower(profileJoin.get("email")), searchTerm)
                );
            });
        }

        if (role != null && !role.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("role"), Role.fromString(role.toUpperCase()))
            );
        }

        if (branchId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("managedBranch").get("id"), branchId)
            );
        }

        return userRepository.findAll(spec, pageable).map(this::toSummaryDTO);
    }


    @Override
    @Transactional
    public UserDTO updateUser(Long id, UserUpdateDTO userUpdateDTO) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));
        User targetUser = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        if (targetUser.isDeleted()) {
            throw new AccessDeniedException("No se puede modificar un usuario que ha sido eliminado.");
        }

        if (!canUpdate(currentUser, targetUser)) {
            throw new AccessDeniedException("No tienes permiso para actualizar este usuario.");
        }

        if (userUpdateDTO.getActive() != null) {
            targetUser.setActive(userUpdateDTO.getActive());
        }

        if (userUpdateDTO.getBypassAccessRules() != null) {
            boolean wasBypassingRules = targetUser.isBypassAccessRules();
            boolean isBypassingRulesNew = userUpdateDTO.getBypassAccessRules();

            // Lógica de protección: si se está quitando el bypass
            if (wasBypassingRules && !isBypassingRulesNew) {
                if (ruleRepository.findByUserId(targetUser.getId()).isEmpty()) {
                    throw new IllegalArgumentException("No se puede restringir el acceso por horario a un usuario que no tiene horarios asignados. Por favor, asigne al menos una regla de acceso primero.");
                }
            }

            // Lógica de limpieza: si se está añadiendo el bypass
            if (!wasBypassingRules && isBypassingRulesNew) {
                targetUser.getAccessRules().clear();
            }
            targetUser.setBypassAccessRules(isBypassingRulesNew);
        }

        if (currentUser.getRole() == Role.DEVELOPER && userUpdateDTO.getRole() != null) {
            Role newRole = Role.fromString(userUpdateDTO.getRole());
            if (newRole == null) throw new IllegalArgumentException("Rol no válido.");
            targetUser.setRole(newRole);
        }

        if (userUpdateDTO.getManagedBranchId() != null) {
            // Si el ID es diferente de null, procedemos a actualizar.

            // Usamos el valor 0 como señal para desasignar (null). Es un "número mágico",
            // pero resuelve la ambigüedad del null en un PATCH.
            if (userUpdateDTO.getManagedBranchId() == 0L) {
                targetUser.setManagedBranch(null);
            } else {
                // Si es cualquier otro número, buscamos y asignamos la sucursal.
                Branch managedBranch = branchRepository.findById(userUpdateDTO.getManagedBranchId())
                        .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con ID: " + userUpdateDTO.getManagedBranchId()));
                targetUser.setManagedBranch(managedBranch);
            }
        }

        targetUser.setUpdatedBy(currentUser);

        return toDTO(userRepository.save(targetUser));
    }

    @Override
    @Transactional
    public void updateOwnPassword(UserPasswordUpdateDTO passwordUpdateDTO) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));

        if (!passwordEncoder.matches(passwordUpdateDTO.getCurrentPassword(), currentUser.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta.");
        }
        if (passwordUpdateDTO.getCurrentPassword().equals(passwordUpdateDTO.getNewPassword())) {
            throw new IllegalArgumentException("La nueva contraseña no puede ser igual a la contraseña actual.");
        }

        currentUser.setPassword(passwordEncoder.encode(passwordUpdateDTO.getNewPassword()));
        currentUser.setPasswordChangedAt(LocalDateTime.now());
        currentUser.setUpdatedBy(currentUser);
        userRepository.save(currentUser);
    }

    @Override
    @Transactional
    public void updateUserPassword(Long userId, AdminPasswordUpdateDTO passwordUpdateDTO) { // <-- CAMBIO en el parámetro
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        if (targetUser.isDeleted()) {
            throw new AccessDeniedException("No se puede modificar la contraseña de un usuario que ha sido eliminado.");
        }

        if (!canUpdate(currentUser, targetUser)) {
            throw new AccessDeniedException("No tienes permiso para cambiar la contraseña de este usuario.");
        }

        targetUser.setPassword(passwordEncoder.encode(passwordUpdateDTO.getNewPassword()));
        targetUser.setPasswordChangedAt(LocalDateTime.now());
        targetUser.setUpdatedBy(currentUser);
        userRepository.save(targetUser);
    }

    @Override
    @Transactional
    public UserDTO updateOwnProfile(UserProfileUpdateDTO profileUpdateDTO) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new ResourceNotFoundException("Usuario no autenticado."));
        UserProfile profile = currentUser.getProfile();

        if (profile == null) {
            throw new ResourceNotFoundException("El perfil para el usuario actual no existe.");
        }

        // Lógica de actualización parcial: solo se actualizan los campos proporcionados
        if (profileUpdateDTO.getEmail() != null) {
            profile.setEmail(profileUpdateDTO.getEmail());
        }
        if (profileUpdateDTO.getPhone() != null) {
            profile.setPhone(profileUpdateDTO.getPhone());
        }

        profile.setUpdatedBy(currentUser);
        userProfileRepository.save(profile);

        return toDTO(currentUser);
    }

    @Override
    @Transactional
    public UserDTO updateUserProfile(Long userId, UserProfileUpdateDTO profileUpdateDTO) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        if (targetUser.isDeleted()) {
            throw new AccessDeniedException("No se puede modificar el perfil de un usuario que ha sido eliminado.");
        }

        if (!canUpdate(currentUser, targetUser)) {
            throw new AccessDeniedException("No tienes permiso para actualizar el perfil de este usuario.");
        }

        UserProfile profile = targetUser.getProfile();
        if (profile == null) {
            throw new ResourceNotFoundException("El perfil para el usuario con ID: " + userId + " no existe.");
        }

        // Actualiza solo los campos que no son nulos
        if (profileUpdateDTO.getFirstName() != null) {
            profile.setFirstName(profileUpdateDTO.getFirstName());
        }

        if (profileUpdateDTO.getLastName() != null) {
            profile.setLastName(profileUpdateDTO.getLastName());
        }

        if (profileUpdateDTO.getEmail() != null) {
            profile.setEmail(profileUpdateDTO.getEmail());
        }

        if (profileUpdateDTO.getPhone() != null) {
            profile.setPhone(profileUpdateDTO.getPhone());
        }

        if (profileUpdateDTO.getAddress() != null) {
            profile.setAddress(profileUpdateDTO.getAddress());
        }

        if (profileUpdateDTO.getEmployeeNumber() != null) {
            profile.setEmployeeNumber(profileUpdateDTO.getEmployeeNumber());
        }

        if (profileUpdateDTO.getHireDate() != null) {
            profile.setHireDate(profileUpdateDTO.getHireDate());
        }

        if (profileUpdateDTO.getTerminationDate() != null) {
            profile.setTerminationDate(profileUpdateDTO.getTerminationDate());
        }

        profile.setUpdatedBy(currentUser);
        userProfileRepository.save(profile);

        return toDTO(targetUser);
    }

    @Override
    @Transactional
    public UserDTO updateDriverDetails(Long userId, DriverDetailUpdateDTO driverDetailUpdateDTO) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        if (targetUser.isDeleted()) {
            throw new AccessDeniedException("No se pueden modificar los detalles de un usuario que ha sido eliminado.");
        }

        if (!canUpdate(currentUser, targetUser)) {
            throw new AccessDeniedException("No tienes permiso para actualizar los detalles de este chófer.");
        }
        if (targetUser.getRole() != Role.DRIVER) {
            throw new IllegalArgumentException("Este usuario no tiene el rol de DRIVER.");
        }

        DriverDetail driverDetail = targetUser.getDriverDetail();
        if (driverDetail == null) {
            throw new ResourceNotFoundException("Los detalles de chófer para el usuario con ID: " + userId + " no existen.");
        }

        if (driverDetailUpdateDTO.getLicenseNumber() != null) {
            driverDetail.setLicenseNumber(driverDetailUpdateDTO.getLicenseNumber());
        }

        if (driverDetailUpdateDTO.getLicenseExpirationDate() != null) {
            driverDetail.setLicenseExpirationDate(driverDetailUpdateDTO.getLicenseExpirationDate());
        }

        driverDetail.setUpdatedBy(currentUser);
        driverDetailRepository.save(driverDetail);

        return toDTO(targetUser);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));
        User targetUser = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        if (!canDelete(currentUser, targetUser)) {
            throw new AccessDeniedException("No tienes permiso para eliminar este usuario.");
        }

        targetUser.setDeleted(true);
        targetUser.setActive(false);
        targetUser.setDeletedAt(LocalDateTime.now());
        targetUser.setDeletedBy(currentUser);

        userRepository.save(targetUser);
    }

    @Override
    @Transactional
    public UserDTO restoreUserById(Long id) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        // Validación 1: Verificar que el usuario esté realmente eliminado
        if (!targetUser.isDeleted()) {
            throw new IllegalArgumentException("El usuario con ID: " + id + " no está eliminado y no puede ser restaurado.");
        }

        // Validación 2: Verificar permisos jerárquicos
        if (!canUpdate(currentUser, targetUser)) {
            throw new AccessDeniedException("No tienes permiso para restaurar a este usuario.");
        }

        // Revertir los campos del borrado lógico
        targetUser.setDeleted(false);
        targetUser.setActive(true); // Se reactiva la cuenta al restaurarla
        targetUser.setDeletedAt(null);
        targetUser.setDeletedBy(null);

        // Actualizar la auditoría de modificación
        targetUser.setUpdatedBy(currentUser);

        User restoredUser = userRepository.save(targetUser);
        return toDTO(restoredUser);
    }

    // --- Métodos de Lógica de Seguridad ---
    private boolean canCreate(Role currentUserRole, Role roleToCreate) {
        if (roleToCreate == Role.DEVELOPER) return false;
        if (currentUserRole == Role.DEVELOPER) return true;
        if (currentUserRole == Role.ADMIN) return roleToCreate.getLevel() < currentUserRole.getLevel();

        return false;
    }
    private boolean canRead(User currentUser, User targetUser) {
        if (currentUser.getId().equals(targetUser.getId())) return true;
        if (targetUser.getRole() == Role.DEVELOPER) return false;

        Role currentUserRole = currentUser.getRole();

        if (currentUserRole == Role.DEVELOPER) return true;
        if (currentUserRole == Role.ADMIN) return targetUser.getRole().getLevel() < currentUserRole.getLevel();

        return false;
    }
    private boolean canUpdate(User currentUser, User targetUser) {
        if (currentUser.getId().equals(targetUser.getId())) return false;
        if (targetUser.getRole() == Role.DEVELOPER) return false;

        Role currentUserRole = currentUser.getRole();

        if (currentUserRole == Role.DEVELOPER) return true;
        if (currentUserRole == Role.ADMIN) return targetUser.getRole().getLevel() < currentUserRole.getLevel();

        return false;
    }
    private boolean canDelete(User currentUser, User targetUser) {
        if (currentUser.getId().equals(targetUser.getId())) return false;
        if (targetUser.getRole() == Role.DEVELOPER) return false;

        Role currentUserRole = currentUser.getRole();

        if (currentUserRole == Role.DEVELOPER) return true;
        if (currentUserRole == Role.ADMIN) return targetUser.getRole().getLevel() < currentUserRole.getLevel();

        return false;
    }

    // --- Métodos Auxiliares y de Mapeo ---
    private UserSummaryDTO toSummaryDTO(User user) {
        if (user == null) return null;

        String fullName = (user.getProfile() != null)
                ? user.getProfile().getFirstName() + " " + user.getProfile().getLastName()
                : "N/A";

        String branchName = (user.getManagedBranch() != null)
                ? user.getManagedBranch().getName()
                : null;

        // --- LÍNEA A MODIFICAR ---
        return new UserSummaryDTO(
                user.getId(),
                fullName,
                user.getUsername(),
                user.getRole().name(),
                user.isActive(),
                branchName,
                user.isDeleted()
        );
    }
    private UserProfileViewDTO toUserProfileViewDTO(User user) {

        if (user == null) return null;

        UserProfileViewDTO dto = new UserProfileViewDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole().name());
        dto.setActive(user.isActive());
        dto.setBypassAccessRules(user.isBypassAccessRules());

        if (user.getManagedBranch() != null) {
            dto.setManagedBranchName(user.getManagedBranch().getName());
        }

        if (user.getProfile() != null) {
            UserDTO.ProfileInfo profileInfo = new UserDTO.ProfileInfo();
            profileInfo.setFirstName(user.getProfile().getFirstName());
            profileInfo.setLastName(user.getProfile().getLastName());
            profileInfo.setEmail(user.getProfile().getEmail());
            profileInfo.setPhone(user.getProfile().getPhone());
            profileInfo.setAddress(user.getProfile().getAddress());
            profileInfo.setEmployeeNumber(user.getProfile().getEmployeeNumber());
            profileInfo.setHireDate(user.getProfile().getHireDate());
            profileInfo.setTerminationDate(user.getProfile().getTerminationDate());
            dto.setProfile(profileInfo);
        }

        if (user.getDriverDetail() != null) {
            UserDTO.DriverInfo driverInfo = new UserDTO.DriverInfo();
            driverInfo.setLicenseNumber(user.getDriverDetail().getLicenseNumber());
            driverInfo.setLicenseExpirationDate(user.getDriverDetail().getLicenseExpirationDate());
            dto.setDriverDetails(driverInfo);
        }

        dto.setAccessRules(user.getAccessRules() != null ?
                user.getAccessRules().stream().map(this::toAccessRuleProfileView).collect(Collectors.toList()) :
                Collections.emptyList());

        return dto;
    }

    private UserProfileViewDTO.AccessRuleProfileView toAccessRuleProfileView(UserAccessRule rule) {
        if (rule == null) return null;
        UserProfileViewDTO.AccessRuleProfileView dto = new UserProfileViewDTO.AccessRuleProfileView();
        dto.setId(rule.getId());
        dto.setDayOfWeek(rule.getDayOfWeek().toString());
        dto.setStartTime(rule.getStartTime());
        dto.setEndTime(rule.getEndTime());
        dto.setAccessTimezone(rule.getAccessTimezone());
        dto.setActive(rule.isActive());
        return dto;
    }

    private UserDTO toDTO(User user) {

        if (user == null) return null;

        UserDTO userDTO = new UserDTO(); // La variable se llama userDTO
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setRole(user.getRole().name());
        userDTO.setActive(user.isActive());
        userDTO.setBypassAccessRules(user.isBypassAccessRules());

        if (user.getManagedBranch() != null) {
            userDTO.setManagedBranchId(user.getManagedBranch().getId());
            userDTO.setManagedBranchName(user.getManagedBranch().getName());
        }

        userDTO.setCreatedAt(user.getCreatedAt());
        userDTO.setLastUpdatedAt(user.getLastUpdatedAt());

        if (user.getCreatedBy() != null) {
            userDTO.setCreatedById(user.getCreatedBy().getId());
            userDTO.setCreatedByUsername(user.getCreatedBy().getUsername());

            if (user.getCreatedBy().getProfile() != null) {
                String fullName = user.getCreatedBy().getProfile().getFirstName() + " " + user.getCreatedBy().getProfile().getLastName();
                userDTO.setCreatedByFullName(fullName);
            }
        }

        if (user.getUpdatedBy() != null) {
            userDTO.setUpdatedById(user.getUpdatedBy().getId());
            userDTO.setUpdatedByUsername(user.getUpdatedBy().getUsername());

            if (user.getUpdatedBy().getProfile() != null) {
                String fullName = user.getUpdatedBy().getProfile().getFirstName() + " " + user.getUpdatedBy().getProfile().getLastName();
                userDTO.setUpdatedByFullName(fullName);
            }
        }

        if (user.getProfile() != null) {
            UserDTO.ProfileInfo profileInfo = new UserDTO.ProfileInfo();
            profileInfo.setFirstName(user.getProfile().getFirstName());
            profileInfo.setLastName(user.getProfile().getLastName());
            profileInfo.setEmail(user.getProfile().getEmail());
            profileInfo.setPhone(user.getProfile().getPhone());
            profileInfo.setAddress(user.getProfile().getAddress());
            profileInfo.setEmployeeNumber(user.getProfile().getEmployeeNumber());
            profileInfo.setHireDate(user.getProfile().getHireDate());
            profileInfo.setTerminationDate(user.getProfile().getTerminationDate());

            userDTO.setProfile(profileInfo);
        }

        if (user.getDriverDetail() != null) {
            UserDTO.DriverInfo driverInfo = new UserDTO.DriverInfo();

            driverInfo.setLicenseNumber(user.getDriverDetail().getLicenseNumber());
            driverInfo.setLicenseExpirationDate(user.getDriverDetail().getLicenseExpirationDate());

            userDTO.setDriverDetails(driverInfo);
        }

        userDTO.setAccessRules(user.getAccessRules() != null ?
                user.getAccessRules().stream().map(this::toAccessRuleDTO).collect(Collectors.toList()) :
                Collections.emptyList());

        if (user.isDeleted()) {
            userDTO.setDeletedAt(user.getDeletedAt());

            if (user.getDeletedBy() != null) {
                userDTO.setDeletedById(user.getDeletedBy().getId());
                userDTO.setDeletedByUsername(user.getDeletedBy().getUsername());

                if (user.getDeletedBy().getProfile() != null) {
                    String fullName = user.getDeletedBy().getProfile().getFirstName() + " " + user.getDeletedBy().getProfile().getLastName();
                    userDTO.setDeletedByFullName(fullName);
                }
            }
        }
        return userDTO;
    }

    private void mapBaseUserDtoToEntity(UserDTO userDTO, User user) {

        user.setUsername(userDTO.getUsername());
        Role role = Role.fromString(userDTO.getRole());

        if (role == null) throw new IllegalArgumentException("El rol '" + userDTO.getRole() + "' no es válido.");
        user.setRole(role);
        user.setActive(userDTO.isActive());
        user.setBypassAccessRules(userDTO.isBypassAccessRules());

        if (userDTO.getManagedBranchId() != null) {
            Branch managedBranch = branchRepository.findById(userDTO.getManagedBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con ID: " + userDTO.getManagedBranchId()));
            user.setManagedBranch(managedBranch);
        } else {
            user.setManagedBranch(null);
        }
    }

    private void mapProfileDtoToEntity(UserDTO.ProfileInfo profileDto, UserProfile profile) {
        profile.setFirstName(profileDto.getFirstName());
        profile.setLastName(profileDto.getLastName());
        profile.setEmail(profileDto.getEmail());
        profile.setPhone(profileDto.getPhone());
        profile.setAddress(profileDto.getAddress());
        profile.setEmployeeNumber(profileDto.getEmployeeNumber());
        profile.setHireDate(profileDto.getHireDate());
    }
    private void mapDriverDetailDtoToEntity(UserDTO.DriverInfo driverDto, DriverDetail driverDetail) {
        driverDetail.setLicenseNumber(driverDto.getLicenseNumber());
        driverDetail.setLicenseExpirationDate(driverDto.getLicenseExpirationDate());
    }
    private UserAccessRuleDTO toAccessRuleDTO(UserAccessRule rule) {
        return new UserAccessRuleDTO(rule.getId(), rule.getUser().getId(), rule.getDayOfWeek().toString(), rule.getStartTime(), rule.getEndTime(), rule.getAccessTimezone(), rule.isActive());
    }
    private Optional<User> getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return Optional.empty();
        }
        Long userId = ((UserDetailsImpl) authentication.getPrincipal()).getId();
        return userRepository.findById(userId);
    }
}