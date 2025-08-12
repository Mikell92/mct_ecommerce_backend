package com.muebleria.mctecommercebackend.dto;

import lombok.Data;

@Data
public class UserUpdateDTO {

    private String role;
    private boolean active;
    private boolean bypassAccessRules;
    private Long managedBranchId;
}