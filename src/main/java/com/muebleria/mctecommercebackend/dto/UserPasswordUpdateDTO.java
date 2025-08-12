package com.muebleria.mctecommercebackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserPasswordUpdateDTO {
    @NotBlank
    @Size(min = 6, message = "La nueva contraseña debe tener al menos 6 caracteres.")
    private String newPassword;
}