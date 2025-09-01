package com.muebleria.mctecommercebackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileViewDTO {

    // --- Datos de la cuenta ---
    private Long id;
    private String username;
    private String role;
    private boolean active;
    private boolean bypassAccessRules;

    // --- ID y Nombre de la Sucursal ---
    private Long managedBranchId;
    private String managedBranchName;

    // --- Relaciones ---
    private UserDTO.ProfileInfo profile;
    private UserDTO.DriverInfo driverDetails;
    private List<UserAccessRuleDTO> accessRules;
}