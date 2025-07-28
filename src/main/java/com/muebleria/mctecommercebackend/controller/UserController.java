package com.muebleria.mctecommercebackend.controller;

import com.muebleria.mctecommercebackend.dto.UserDTO;
import com.muebleria.mctecommercebackend.model.User;
import com.muebleria.mctecommercebackend.service.UserService;
import jakarta.validation.Valid; // Importa para habilitar las validaciones en los DTOs
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // Importar Page
import org.springframework.data.domain.Pageable; // Importar Pageable
import org.springframework.data.web.PageableDefault; // Importar PageableDefault para valores por defecto
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.MethodArgumentNotValidException; // Para manejar errores de validación
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ExceptionHandler; // Para manejo de excepciones
import org.springframework.web.bind.annotation.ResponseStatus; // Para manejar excepciones

import java.util.HashMap;
import java.util.Map;

@RestController // Indica que esta clase es un controlador REST de Spring
@RequestMapping("/api/users") // Define la ruta base para todos los endpoints en este controlador
public class UserController {

    private final UserService userService;

    @Autowired // Inyecta el UserService para usar sus métodos de negocio
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Endpoint para crear un nuevo usuario
    // Solo los usuarios con rol 'ADMIN' pueden crear usuarios
    // POST: /api/users
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // Requiere el rol ADMIN
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserDTO userDTO) {
        // @Valid: Activa las anotaciones de validación (ej. @NotBlank, @Size) en el UserDTO.
        // Si las validaciones fallan, Spring capturará el error antes de que el método se ejecute.
        // @RequestBody: Mapea el cuerpo JSON de la solicitud HTTP a un objeto UserDTO.
        User savedUser = userService.saveUser(userDTO);

        // Convertir la entidad User guardada a un DTO para la respuesta.
        // Es buena práctica no retornar la entidad directamente ni el passwordHash.
        UserDTO responseDTO = new UserDTO(
                savedUser.getUserId(),
                savedUser.getUsername(),
                null, // La contraseña nunca se retorna en la respuesta de la API
                savedUser.getRole(),
                savedUser.getIsDeleted()
        );
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED); // Retorna 201 Created si es exitoso
    }

    // Endpoint para obtener un usuario por ID
    // GET: /api/users/{id}
    // Solo los usuarios con rol 'ADMIN', 'GESTOR_INVENTARIO' pueden ver usuarios
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_INVENTARIO')") // Permite múltiples roles
    public ResponseEntity<UserDTO> getUserById(@PathVariable Integer id) {
        // @PathVariable: Extrae el valor del ID de la URL (ej. de /api/users/123, extrae 123)
        return userService.findById(id)
                .map(user -> {
                    // Si se encuentra el usuario, lo mapea a un DTO para la respuesta
                    UserDTO responseDTO = new UserDTO(
                            user.getUserId(),
                            user.getUsername(),
                            null,
                            user.getRole(),
                            user.getIsDeleted()
                    );
                    return new ResponseEntity<>(responseDTO, HttpStatus.OK); // Retorna 200 OK
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND)); // Si no se encuentra, retorna 404 Not Found
    }

    /**
     * **Modificado:** Endpoint para obtener todos los usuarios paginados.
     * Permite los roles 'ADMIN'.
     *
     * @param pageable Objeto Pageable inyectado automáticamente por Spring,
     * se puede configurar con parámetros como `?page=0&size=10&sort=username,asc`.
     * `@PageableDefault` permite definir valores por defecto.
     * @return ResponseEntity con una Page de UserDTOs y estado 200 OK.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserDTO>> getAllUsers(@PageableDefault(size = 10, sort = "username") Pageable pageable) {
        Page<User> usersPage = userService.findAllUsers(pageable);

        // Mapea la Page de entidades User a una Page de UserDTOs
        Page<UserDTO> userDTOsPage = usersPage.map(user -> new UserDTO(
                user.getUserId(),
                user.getUsername(),
                null, // Contraseña no se expone
                user.getRole(),
                user.getIsDeleted()
        ));
        return new ResponseEntity<>(userDTOsPage, HttpStatus.OK);
    }

    // Endpoint para actualizar un usuario existente
    // Solo los usuarios con rol 'ADMIN' pueden actualizar usuarios
    // PUT: /api/users/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Integer id, @Valid @RequestBody UserDTO userDTO) {
        // @Valid también se aplica aquí para asegurar que los datos de actualización son válidos
        User updatedUser = userService.updateUser(id, userDTO);
        // Mapea el usuario actualizado a un DTO para la respuesta
        UserDTO responseDTO = new UserDTO(
                updatedUser.getUserId(),
                updatedUser.getUsername(),
                null,
                updatedUser.getRole(),
                updatedUser.getIsDeleted()
        );
        return new ResponseEntity<>(responseDTO, HttpStatus.OK); // Retorna 200 OK
    }

    // Endpoint para eliminar un usuario (borrado lógico)
    // Solo los usuarios con rol 'ADMIN' pueden eliminar usuarios
    // DELETE: /api/users/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT); // Retorna 204 No Content (indica éxito sin contenido de respuesta)
    }

    // **Manejo Global de Excepciones para Validaciones**
    // Este método se ejecutará automáticamente cuando una validación @Valid falle en cualquier controlador
    @ResponseStatus(HttpStatus.BAD_REQUEST) // Establece el código de estado HTTP a 400 Bad Request
    @ExceptionHandler(MethodArgumentNotValidException.class) // Captura esta excepción específica de validación
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((org.springframework.validation.FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors; // Retorna un mapa con los nombres de los campos y sus mensajes de error
    }

    // **Manejo de Excepciones para Errores de Negocio (ej. usuario ya existe)**
    // Esto captura la RuntimeException que lanzamos en el servicio si el usuario ya existe.
    @ResponseStatus(HttpStatus.CONFLICT) // Establece el código de estado HTTP a 409 Conflict
    @ExceptionHandler(RuntimeException.class)
    public Map<String, String> handleBusinessExceptions(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return error;
    }
}