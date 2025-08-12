package com.muebleria.mctecommercebackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UserProfileUpdateDTO {
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