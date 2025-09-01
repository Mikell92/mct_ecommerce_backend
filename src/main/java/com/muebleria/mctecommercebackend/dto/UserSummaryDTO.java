package com.muebleria.mctecommercebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDTO {
    private Long id;
    private String fullName;
    private String username;
    private String role;
    private boolean active;
    private String managedBranchName;
}