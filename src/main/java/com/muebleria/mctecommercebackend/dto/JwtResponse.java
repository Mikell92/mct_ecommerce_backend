package com.muebleria.mctecommercebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer"; // Tipo de token, por defecto "Bearer"
    private Integer userId;
    private String username;
    private String role; // El rol del usuario
}