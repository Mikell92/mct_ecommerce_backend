package com.muebleria.mctecommercebackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchDTO {

    private Long id;

    @NotBlank(message = "El nombre de la sucursal no puede estar vacío.")
    private String name;

    @NotBlank(message = "La dirección de la sucursal no puede estar vacía.")
    private String address;

    private String phone;

    private String rfc;

    @NotBlank(message = "El prefijo de la orden no puede estar vacío.")
    private String orderPrefix;

    private int lastOrderSequenceNumber;
}