package com.muebleria.mctecommercebackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
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

    // --- Nombre de la Sucursal ---
    private String managedBranchName;

    // --- Relaciones ---
    private UserDTO.ProfileInfo profile;
    private UserDTO.DriverInfo driverDetails;

    // DTO anidado AccessRuleProfileView.
    private List<AccessRuleProfileView> accessRules;

    @Data
    public static class AccessRuleProfileView {
        private Long id;
        private String dayOfWeek;
        private LocalTime startTime;
        private LocalTime endTime;
        private String accessTimezone;
        private boolean active;
    }
}