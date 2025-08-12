package com.muebleria.mctecommercebackend.service.impl;

import com.muebleria.mctecommercebackend.dto.*;
import com.muebleria.mctecommercebackend.exception.ResourceNotFoundException;
import com.muebleria.mctecommercebackend.model.*;
import com.muebleria.mctecommercebackend.repository.*;
import com.muebleria.mctecommercebackend.security.user.UserDetailsImpl;
import com.muebleria.mctecommercebackend.service.UserService;
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
import java.util.List;
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
        savedUser.setProfile(profile); // Sincronizar el objeto en memoria

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
            savedUser.setDriverDetail(driverDetail); // Sincronizar el objeto en memoria
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
                savedUser.getAccessRules().add(rule); // Sincronizar la colección en memoria
            }
        }

        return toDTO(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDTO> findById(Long id) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));
        Optional<User> targetUserOpt = userRepository.findById(id);
        targetUserOpt.ifPresent(targetUser -> {
            if (!canRead(currentUser, targetUser)) {
                throw new AccessDeniedException("No tienes permiso para ver este usuario.");
            }
        });
        return targetUserOpt.map(this::toDTO);
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
    public Page<UserDTO> findAll(Pageable pageable, UserStatus status) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));
        Specification<User> spec = (root, query, cb) -> cb.conjunction();
        spec = spec.and((root, query, cb) -> cb.notEqual(root.get("id"), currentUser.getId()));
        Role currentUserRole = currentUser.getRole();
        if (currentUserRole == Role.DEVELOPER) {
            spec = spec.and((root, query, cb) -> cb.notEqual(root.get("role"), Role.DEVELOPER));
        } else if (currentUserRole == Role.ADMIN) {
            spec = spec.and((root, query, cb) -> cb.notEqual(root.get("role"), Role.DEVELOPER));
            spec = spec.and((root, query, cb) -> cb.notEqual(root.get("role"), Role.ADMIN));
        }
        if (status == UserStatus.ACTIVE) {
            spec = spec.and((root, query, cb) -> cb.isTrue(root.get("active")));
        } else if (status == UserStatus.INACTIVE) {
            spec = spec.and((root, query, cb) -> cb.isFalse(root.get("active")));
        }
        return userRepository.findAll(spec, pageable).map(this::toDTO);
    }

    @Override
    @Transactional
    public UserDTO updateUser(Long id, UserUpdateDTO userUpdateDTO) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));
        User targetUser = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        if (!canUpdate(currentUser, targetUser)) {
            throw new AccessDeniedException("No tienes permiso para actualizar este usuario.");
        }

        boolean wasBypassingRules = targetUser.isBypassAccessRules();

        if (wasBypassingRules && !userUpdateDTO.isBypassAccessRules()) {
            if (ruleRepository.findByUserId(targetUser.getId()).isEmpty()) {
                throw new IllegalArgumentException("No se puede restringir el acceso por horario a un usuario que no tiene horarios asignados. Por favor, asigne al menos una regla de acceso primero.");
            }
        }

        targetUser.setActive(userUpdateDTO.isActive());
        targetUser.setBypassAccessRules(userUpdateDTO.isBypassAccessRules());

        if (currentUser.getRole() == Role.DEVELOPER && userUpdateDTO.getRole() != null) {
            Role newRole = Role.fromString(userUpdateDTO.getRole());
            if (newRole == null) throw new IllegalArgumentException("Rol no válido.");
            targetUser.setRole(newRole);
        }

        if (userUpdateDTO.getManagedBranchId() != null) {
            Branch managedBranch = branchRepository.findById(userUpdateDTO.getManagedBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con ID: " + userUpdateDTO.getManagedBranchId()));
            targetUser.setManagedBranch(managedBranch);
        } else {
            targetUser.setManagedBranch(null);
        }

        if (!wasBypassingRules && userUpdateDTO.isBypassAccessRules()) {
            //ruleRepository.deleteByUserId(targetUser.getId());
            targetUser.getAccessRules().clear();
        }

        targetUser.setUpdatedBy(currentUser);
        return toDTO(userRepository.save(targetUser));
    }

    @Override
    @Transactional
    public void updateOwnPassword(UserPasswordUpdateDTO passwordUpdateDTO) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        currentUser.setPassword(passwordEncoder.encode(passwordUpdateDTO.getNewPassword()));
        currentUser.setUpdatedBy(currentUser);
        userRepository.save(currentUser);
    }

    @Override
    @Transactional
    public UserDTO updateUserProfile(Long userId, UserProfileUpdateDTO profileUpdateDTO) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        if (!canUpdate(currentUser, targetUser)) {
            throw new AccessDeniedException("No tienes permiso para actualizar el perfil de este usuario.");
        }

        UserProfile profile = targetUser.getProfile();
        if (profile == null) {
            throw new ResourceNotFoundException("El perfil para el usuario con ID: " + userId + " no existe.");
        }

        profile.setFirstName(profileUpdateDTO.getFirstName());
        profile.setLastName(profileUpdateDTO.getLastName());
        profile.setEmail(profileUpdateDTO.getEmail());
        profile.setPhone(profileUpdateDTO.getPhone());
        profile.setAddress(profileUpdateDTO.getAddress());
        profile.setEmployeeNumber(profileUpdateDTO.getEmployeeNumber());
        profile.setHireDate(profileUpdateDTO.getHireDate());
        profile.setTerminationDate(profileUpdateDTO.getTerminationDate());
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

        driverDetail.setLicenseNumber(driverDetailUpdateDTO.getLicenseNumber());
        driverDetail.setLicenseExpirationDate(driverDetailUpdateDTO.getLicenseExpirationDate());
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

    // --- Métodos Auxiliares ---
    private UserDTO toDTO(User user) {
        if (user == null) return null;
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setRole(user.getRole().name());
        userDTO.setActive(user.isActive());
        userDTO.setBypassAccessRules(user.isBypassAccessRules());
        if (user.getManagedBranch() != null) {
            userDTO.setManagedBranchId(user.getManagedBranch().getId());
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
        List<UserAccessRuleDTO> ruleDTOs = user.getAccessRules() != null ?
                user.getAccessRules().stream().map(this::toAccessRuleDTO).collect(Collectors.toList()) :
                Collections.emptyList();
        userDTO.setAccessRules(ruleDTOs);
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