package com.muebleria.mctecommercebackend.dto;

import jakarta.validation.constraints.NotBlank; // Importa para validar que un campo no esté vacío (ni solo espacios)
import jakarta.validation.constraints.Size;    // Importa para validar la longitud de una cadena
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Genera getters, setters, toString, equals y hashCode de Lombok
@NoArgsConstructor // Genera un constructor sin argumentos
@AllArgsConstructor // Genera un constructor con todos los argumentos
public class UserDTO {

    private Integer userId; // Usamos Integer para el ID. No se valida aquí porque es generado por la BD o viene de la URL para updates.

    // **Validaciones para 'username'**
    @NotBlank(message = "El nombre de usuario no puede estar vacío.")
    @Size(min = 3, max = 100, message = "El nombre de usuario debe tener entre 3 y 100 caracteres.")
    private String username;

    // **Validaciones para 'password'** (solo para entrada, se hasheará y no se devolverá)
    @NotBlank(message = "La contraseña no puede estar vacía.")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres.")
    private String password;

    // **Validaciones para 'role'**
    @NotBlank(message = "El rol no puede estar vacío.")
    private String role;

    // **Validación para 'isDeleted'**
    private Boolean isDeleted;
}