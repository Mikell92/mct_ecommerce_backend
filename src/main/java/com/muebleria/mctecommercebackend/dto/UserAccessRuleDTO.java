package com.muebleria.mctecommercebackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAccessRuleDTO {

    private Long id;

    // Se usará para las respuestas, pero no para las peticiones de creación,
    // ya que el ID del usuario vendrá en la URL.
    private Long userId;

    @NotBlank(message = "El día de la semana no puede estar vacío.")
    @Pattern(regexp = "MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY", message = "El día de la semana no es válido.")
    private String dayOfWeek;

    @NotNull(message = "La hora de inicio no puede ser nula.")
    private LocalTime startTime;

    @NotNull(message = "La hora de fin no puede ser nula.")
    private LocalTime endTime;

    @NotBlank(message = "La zona horaria no puede estar vacía.")
    private String accessTimezone;

    private boolean active = true;
}