package com.muebleria.mctecommercebackend.dto;

import com.muebleria.mctecommercebackend.model.Category; // Importa la entidad Category
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO (Data Transfer Object) para la entidad {@link com.muebleria.mctecommercebackend.model.Category}.
 * <p>
 * Utilizado para transferir datos de categorías y aplicar validaciones en la entrada de la API.
 * </p>
 */
@Data // Genera getters, setters, toString, equals y hashCode
@NoArgsConstructor // Genera un constructor sin argumentos
@AllArgsConstructor // Genera un constructor con todos los argumentos
public class CategoryDTO {

    private Integer categoryId; // El ID de la categoría (puede ser nulo en la creación)

    @NotBlank(message = "El nombre de la categoría no puede estar vacío.")
    @Size(min = 4, max = 100, message = "El nombre de la categoría debe tener entre 4 y 100 caracteres.")
    private String name;

    private Boolean isDeleted; // Para indicar el estado de borrado lógico

    /**
     * Método estático para convertir una entidad Category a un CategoryDTO.
     *
     * @param category La entidad Category a convertir.
     * @return Un nuevo CategoryDTO con los datos de la entidad.
     */
    public static CategoryDTO fromEntity(Category category) {
        return new CategoryDTO(
                category.getCategoryId(),
                category.getName(),
                category.getIsDeleted()
        );
    }

}