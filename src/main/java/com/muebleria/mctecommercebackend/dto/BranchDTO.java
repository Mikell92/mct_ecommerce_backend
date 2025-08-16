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

    private String streetAddress;

    private String addressLine2;

    private String neighborhood;

    private String city;

    private String state;

    private String postalCode;

    private String phone;

    private String rfc;

    @NotBlank(message = "El prefijo de la orden no puede estar vacío.")
    private String orderPrefix;

    private int lastOrderSequenceNumber;
}