package com.muebleria.mctecommercebackend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UserProfileUpdateDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String employeeNumber;
    private LocalDate hireDate;
    private LocalDate terminationDate;
}