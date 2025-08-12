package com.muebleria.mctecommercebackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // No mostrará campos nulos en la respuesta JSON
public class UserDTO {

    // --- Datos de la cuenta (User) ---
    private Long id;
    @NotBlank(message = "El nombre de usuario no puede estar vacío.")
    private String username;
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres.")
    private String password;
    @NotBlank(message = "El rol no puede estar vacío.")
    private String role;
    private boolean active;
    private boolean bypassAccessRules;
    private Long managedBranchId;

    @Valid // Valida los campos dentro de ProfileInfo si está presente
    @NotNull(message = "La información del perfil es obligatoria.")
    private ProfileInfo profile;

    @Valid
    private DriverInfo driverDetails;

    @Valid
    private List<UserAccessRuleDTO> accessRules;

    @Data
    public static class ProfileInfo {
        @NotBlank
        private String firstName;
        @NotBlank
        private String lastName;
        private String email;
        private String phone;
        private String address;
        private String employeeNumber;
        private LocalDate hireDate;
        private LocalDate terminationDate;
    }

    @Data
    public static class DriverInfo {
        @NotBlank
        private String licenseNumber;
        private LocalDate licenseExpirationDate;
    }
}