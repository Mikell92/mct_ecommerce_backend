package com.muebleria.mctecommercebackend.dto;

import com.muebleria.mctecommercebackend.model.Branch;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO (Data Transfer Object) para la entidad Branch.
 * Se utiliza para transferir datos de sucursales entre las capas de la aplicación
 * y para las operaciones de la API (creación, actualización y visualización).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchDTO {

    /**
     * Identificador único de la sucursal.
     * Es útil para respuestas y actualizaciones, no para la creación inicial.
     */
    private Integer branchId;

    /**
     * Nombre de la sucursal.
     * Obligatorio para la creación y actualización.
     */
    private String name;

    /**
     * Dirección de la sucursal.
     * Puede ser nula.
     */
    private String address;

    /**
     * Número de teléfono de la sucursal.
     * Puede ser nulo.
     */
    private String phone;

    /**
     * Prefijo para los números de orden de la sucursal.
     * Obligatorio y único.
     */
    private String orderPrefix;

    /**
     * Último número de secuencia de orden utilizado por esta sucursal.
     * Principalmente para respuestas, se gestiona internamente.
     */
    private Integer lastOrderSequenceNumber;

    /**
     * Constructor para crear un BranchDTO a partir de una entidad Branch.
     *
     * @param branch La entidad Branch de la cual se obtendrán los datos.
     * @return Un nuevo BranchDTO con los datos de la entidad.
     */
    public static BranchDTO fromEntity(Branch branch) {
        BranchDTO dto = new BranchDTO();
        dto.setBranchId(branch.getBranchId());
        dto.setName(branch.getName());
        dto.setAddress(branch.getAddress());
        dto.setPhone(branch.getPhone());
        dto.setOrderPrefix(branch.getOrderPrefix());
        dto.setLastOrderSequenceNumber(branch.getLastOrderSequenceNumber());
        return dto;
    }

    /**
     * Convierte este DTO en una entidad Branch.
     * Útil para operaciones de creación y actualización.
     * Los campos de auditoría y `isDeleted` se manejan en la capa de servicio o por JPA.
     *
     * @return Una nueva entidad Branch con los datos del DTO.
     */
    public Branch toEntity() {
        Branch branch = new Branch();
        branch.setBranchId(this.branchId); // Podría ser nulo en caso de creación
        branch.setName(this.name);
        branch.setAddress(this.address);
        branch.setPhone(this.phone);
        branch.setOrderPrefix(this.orderPrefix);
        // lastOrderSequenceNumber se inicializa en la entidad o se carga de DB
        // isDeleted se inicializa en la entidad o se maneja en el servicio
        return branch;
    }
}