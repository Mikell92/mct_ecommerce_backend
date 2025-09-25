package com.muebleria.mctecommercebackend.dto;

import lombok.Data;

@Data
public class BranchListDTO {
    private Long id;
    private String name;
    private String neighborhood;
    private String city;
    private String state;
}