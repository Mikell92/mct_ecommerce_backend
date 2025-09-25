package com.muebleria.mctecommercebackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BranchDTO {

    private Long id;

    @NotBlank(message = "El nombre de la sucursal no puede estar vacío.")
    private String name;

    private String streetAddress;
    private String addressLine2;
    private String neighborhood;
    private String city;
    private String state;

    @Pattern(regexp = "^[0-9]*$", message = "El código postal solo debe contener números.")
    private String postalCode;

    @Pattern(regexp = "^[0-9]*$", message = "El teléfono solo debe contener números.")
    private String phone;

    @Pattern(regexp = "^[A-Z&Ñ]{3,4}\\d{6}[A-Z0-9]{3}$", message = "El formato del RFC no es válido.")
    private String rfc;

    @NotBlank(message = "El prefijo de la orden no puede estar vacío.")
    private String orderPrefix;

    @NotNull(message = "El último número de secuencia no puede ser nulo.")
    @PositiveOrZero(message = "El último número de secuencia debe ser un número positivo o cero.")
    private int lastOrderSequenceNumber;

    private LocalDateTime createdAt;
    private String createdByFullName;

    private LocalDateTime lastUpdatedAt;
    private String updatedByFullName;

    private LocalDateTime deletedAt;
    private String deletedByFullName;
}