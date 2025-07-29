package com.muebleria.mctecommercebackend.service.impl;

import com.muebleria.mctecommercebackend.dto.UserDTO;
import com.muebleria.mctecommercebackend.exception.ResourceNotFoundException;
import com.muebleria.mctecommercebackend.model.User;
import com.muebleria.mctecommercebackend.model.Branch; // [MEJORA] -> Importa la entidad Branch
import com.muebleria.mctecommercebackend.repository.UserRepository;
import com.muebleria.mctecommercebackend.repository.BranchRepository; // [MEJORA] -> Importa BranchRepository
import com.muebleria.mctecommercebackend.security.user.UserDetailsImpl;
import com.muebleria.mctecommercebackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // Importar Page
import org.springframework.data.domain.PageImpl; // Importar PageImpl
import org.springframework.data.domain.Pageable; // Importar Pageable
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service // Indica que esta clase es un componente de servicio gestionado por Spring
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BranchRepository branchRepository; // [MEJORA] -> Inyección de BranchRepository

    /**
     * Constructor para la inyección de dependencias de Spring.
     *
     * @param userRepository El repositorio de usuarios para la persistencia de datos.
     * @param passwordEncoder El codificador de contraseñas para encriptar las credenciales.
     */

    @Autowired // Inyecta las dependencias necesarias (UserRepository y PasswordEncoder)
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, BranchRepository branchRepository) { // [MEJORA] -> Añade BranchRepository al constructor
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.branchRepository = branchRepository; // [MEJORA] -> Asigna BranchRepository
    }

    /**
     * Método auxiliar para obtener el ID del usuario actualmente autenticado.
     * Utiliza el {@link SecurityContextHolder} para acceder al contexto de seguridad de Spring.
     *
     * @return Un {@link Optional<Integer>} que contiene el ID del usuario si está autenticado,
     * o un Optional vacío si no hay un usuario autenticado o el principal no es un {@link UserDetailsImpl}.
     */

    private Optional<Integer> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return Optional.of(userDetails.getId());
        }
        return Optional.empty();
    }

    /**
     * Guarda un nuevo usuario en la base de datos.
     * Realiza validaciones, encripta la contraseña y asigna el rol.
     * También puebla el campo de auditoría 'created_by_user_id'.
     *
     * @param userDTO El objeto de transferencia de datos con la información del nuevo usuario.
     * @return El objeto {@link User} guardado, con su ID asignado.
     * @throws RuntimeException si el nombre de usuario ya existe.
     */

    @Override
    public User saveUser(UserDTO userDTO) {
        // Lógica de negocio: Puedes añadir validaciones adicionales aquí,
        // por ejemplo, verificar si el nombre de usuario ya existe.
        if (userRepository.findByUsername(userDTO.getUsername()).isPresent()) {
            throw new RuntimeException("El nombre de usuario '" + userDTO.getUsername() + "' ya existe.");
        }

        User user = new User();
        user.setUsername(userDTO.getUsername());
        // **IMPORTANTE: Hashear la contraseña antes de guardarla en la base de datos**
        user.setPasswordHash(passwordEncoder.encode(userDTO.getPassword()));

        // Asegura que el rol siempre empiece con "ROLE_"
        String role = userDTO.getRole().toUpperCase(); // Se recomienda usar mayúsculas
        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }
        user.setRole(role);

        user.setIsDeleted(false); // Por defecto, un nuevo usuario no está eliminado

        // --- [MEJORA] -> Lógica para asignar la sucursal gestionada ---
        if (userDTO.getManagedBranchId() != null) {
            Branch managedBranch = branchRepository.findById(userDTO.getManagedBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sucursal gestionada no encontrada con ID: " + userDTO.getManagedBranchId()));
            user.setManagedBranch(managedBranch);
        } else {
            // Si el managedBranchId es nulo en el DTO, asegúrate de que el campo en User también sea nulo.
            user.setManagedBranch(null);
        }
        // --- FIN MEJORA ---

        // Asignar el ID del usuario que lo crea (Auditoría)
        getCurrentUserId().ifPresent(user::setCreatedByUserId);
        //user.setCreatedAt(LocalDateTime.now()); // [MEJORA] -> Inicializa createdAt
        //user.setLastUpdatedAt(LocalDateTime.now()); // [MEJORA] -> Inicializa lastUpdatedAt


        return userRepository.save(user);
    }

    /**
     * Busca un usuario por su ID.
     *
     * @param id El ID del usuario.
     * @return Un {@link Optional<User>} que contiene el usuario si existe y no está
     * eliminado lógicamente, o un Optional vacío en caso contrario.
     */

    @Override
    public Optional<User> findById(Integer id) {
        // Mejorado para no devolver usuarios eliminados lógicamente
        return userRepository.findById(id)
                .filter(user -> !user.getIsDeleted());
    }

    /**
     * Busca un usuario por su nombre de usuario.
     *
     * @param username El nombre de usuario.
     * @return Un {@link Optional<User>} que contiene el usuario si existe y no está
     * eliminado lógicamente, o un Optional vacío en caso contrario.
     */

    @Override
    public Optional<User> findByUsername(String username) {
        // Mejorado para no devolver usuarios eliminados lógicamente
        return userRepository.findByUsername(username)
                .filter(user -> !user.getIsDeleted());
    }

    /**
     * **Modificado:** Obtiene una página de usuarios activos (no eliminados lógicamente).
     *
     * @param pageable Objeto Pageable con la información de paginación (número de página, tamaño, ordenación).
     * @return Una Page de entidades User activas.
     */
    @Override
    public Page<User> findAllUsers(Pageable pageable) {
        // Obtenemos todos los usuarios del repositorio y los filtramos por isDeleted=false
        List<User> activeUsers = userRepository.findAll().stream()
                .filter(user -> !user.getIsDeleted())
                .collect(Collectors.toList());

        // Implementación de paginación manual sobre la lista filtrada
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), activeUsers.size());
        List<User> pageContent = activeUsers.subList(start, end);

        // Devolvemos un PageImpl que es una implementación de la interfaz Page
        return new PageImpl<>(pageContent, pageable, activeUsers.size());
    }

    /**
     * Actualiza la información de un usuario existente.
     * Realiza validaciones y encripta la nueva contraseña si se proporciona.
     * También puebla el campo de auditoría 'updated_by_user_id'.
     *
     * @param id El ID del usuario a actualizar.
     * @param userDTO El objeto con los nuevos datos del usuario.
     * @return El objeto {@link User} actualizado y guardado.
     * @throws ResourceNotFoundException si el usuario no se encuentra o ya está eliminado lógicamente.
     * @throws RuntimeException si el nuevo nombre de usuario ya está en uso.
     */
    @Override
    public User updateUser(Integer id, UserDTO userDTO) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        // No se debe permitir actualizar un usuario que ya ha sido borrado lógicamente
        if (existingUser.getIsDeleted()) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + id);
        }

        // Valida que el nuevo nombre de usuario no exista
        if (!existingUser.getUsername().equals(userDTO.getUsername()) &&
                userRepository.findByUsername(userDTO.getUsername()).isPresent()) {
            throw new RuntimeException("El nuevo nombre de usuario '" + userDTO.getUsername() + "' ya está en uso.");
        }

        existingUser.setUsername(userDTO.getUsername());

        // Solo actualiza la contraseña si se proporciona una nueva
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            existingUser.setPasswordHash(passwordEncoder.encode(userDTO.getPassword()));
        }

        // Asegura que el rol siempre empiece con "ROLE_"
        String role = userDTO.getRole().toUpperCase();
        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }
        existingUser.setRole(role);

        // --- [MEJORA] -> Lógica para asignar/actualizar la sucursal gestionada ---
        if (userDTO.getManagedBranchId() != null) {
            Branch managedBranch = branchRepository.findById(userDTO.getManagedBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sucursal gestionada no encontrada con ID: " + userDTO.getManagedBranchId()));
            existingUser.setManagedBranch(managedBranch);
        } else {
            // Si el managedBranchId es nulo en el DTO, establece el managedBranch del usuario a nulo.
            // Esto permite desasociar un usuario de una sucursal (ej. convertir un CAJERO_SUCURSAL en ADMIN).
            existingUser.setManagedBranch(null);
        }
        // --- FIN MEJORA ---

        // Auditoría: Asigna el ID del usuario que lo actualiza
        getCurrentUserId().ifPresent(existingUser::setUpdatedByUserId);
        //existingUser.setLastUpdatedAt(LocalDateTime.now()); // Actualiza lastUpdatedAt

        return userRepository.save(existingUser);
    }

    /**
     * Realiza la eliminación lógica de un usuario.
     * En lugar de borrar el registro, marca el campo 'is_deleted' como true
     * y registra la fecha y el usuario que realizó la acción.
     *
     * @param id El ID del usuario a eliminar lógicamente.
     * @throws ResourceNotFoundException si el usuario no se encuentra.
     */
    @Override
    public void deleteUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        user.setIsDeleted(true);
        user.setDeletedAt(LocalDateTime.now());

        // Auditoría: Asigna el ID del usuario que lo elimina
        getCurrentUserId().ifPresent(user::setDeletedByUserId);

        userRepository.save(user);
    }
}