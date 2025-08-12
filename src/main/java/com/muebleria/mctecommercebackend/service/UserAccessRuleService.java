package com.muebleria.mctecommercebackend.service;

import com.muebleria.mctecommercebackend.dto.UserAccessRuleDTO;

import java.util.List;

public interface UserAccessRuleService {

    /**
     * Crea una nueva regla de acceso para un usuario.
     * @param userId El ID del usuario al que se le asignará la regla.
     * @param ruleDTO El DTO con la información de la regla.
     * @return El DTO de la regla creada.
     */
    UserAccessRuleDTO createRuleForUser(Long userId, UserAccessRuleDTO ruleDTO);

    /**
     * Obtiene todas las reglas de acceso de un usuario.
     * @param userId El ID del usuario.
     * @return Una lista de DTOs de las reglas.
     */
    List<UserAccessRuleDTO> getRulesByUserId(Long userId);

    /**
     * Actualiza una regla de acceso existente.
     * @param ruleId El ID de la regla a actualizar.
     * @param ruleDTO El DTO con los nuevos datos.
     * @return El DTO de la regla actualizada.
     */
    UserAccessRuleDTO updateRule(Long ruleId, UserAccessRuleDTO ruleDTO);

    /**
     * Elimina una regla de acceso.
     * @param ruleId El ID de la regla a eliminar.
     */
    void deleteRule(Long ruleId);
}