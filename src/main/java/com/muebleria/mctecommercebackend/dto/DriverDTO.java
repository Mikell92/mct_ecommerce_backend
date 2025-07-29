package com.muebleria.mctecommercebackend.dto;

import com.muebleria.mctecommercebackend.model.Driver;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO (Data Transfer Object) para la entidad {@link Driver}.
 * Se utiliza para transferir datos de chóferes entre las capas de la aplicación
 * y para las operaciones de la API (creación, actualización y visualización).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverDTO {

    /**
     * Identificador único del chófer.
     * Es útil para respuestas y actualizaciones, no para la creación inicial.
     */
    private Integer driverId;

    /**
     * Nombre completo del chófer.
     * Obligatorio para la creación y actualización.
     */
    @NotBlank(message = "El nombre del chófer no puede estar vacío.")
    @Size(min = 3, max = 100, message = "El nombre del chófer debe tener entre 3 y 100 caracteres.")
    private String name;

    /**
     * Número de teléfono de contacto del chófer.
     * Puede ser nulo.
     */
    @Size(max = 20, message = "El teléfono no puede exceder los 20 caracteres.")
    private String phone;

    /**
     * Número de licencia de conducir del chófer.
     * Debe ser único.
     */
    @Size(max = 50, message = "La licencia no puede exceder los 50 caracteres.")
    private String license;

    /**
     * Indica si el chófer está activo y disponible.
     */
    private Boolean isActive;

    /**
     * Constructor para crear un DriverDTO a partir de una entidad Driver.
     * Es útil para formatear la respuesta que se envía al cliente.
     *
     * @param driver La entidad Driver de la cual se obtendrán los datos.
     * @return Un nuevo DriverDTO con los datos de la entidad.
     */
    public static DriverDTO fromEntity(Driver driver) {
        DriverDTO dto = new DriverDTO();
        dto.setDriverId(driver.getDriverId());
        dto.setName(driver.getName());
        dto.setPhone(driver.getPhone());
        dto.setLicense(driver.getLicense());
        dto.setIsActive(driver.getIsActive());
        // No se incluyen campos de auditoría ni isDeleted en este DTO simple para respuestas
        return dto;
    }
}