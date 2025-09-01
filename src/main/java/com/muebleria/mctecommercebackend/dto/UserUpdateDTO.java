package com.muebleria.mctecommercebackend.dto;

import lombok.Data;

@Data
public class UserUpdateDTO {

    private String role;
    private Boolean active;
    private Boolean bypassAccessRules;
    private Long managedBranchId;
}