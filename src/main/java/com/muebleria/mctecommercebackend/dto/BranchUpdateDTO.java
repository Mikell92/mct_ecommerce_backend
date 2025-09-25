package com.muebleria.mctecommercebackend.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class BranchUpdateDTO {

    // Solo incluimos los campos que se pueden modificar
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

    private String orderPrefix;
}