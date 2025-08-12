package com.muebleria.mctecommercebackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

@Data
public class DriverDetailUpdateDTO {
    @NotBlank
    private String licenseNumber;
    private LocalDate licenseExpirationDate;
}