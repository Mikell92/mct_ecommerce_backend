package com.muebleria.mctecommercebackend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad que representa una sucursal en el sistema de mueblería.
 * Mapea a la tabla 'branches' en la base de datos.
 * Incluye campos de auditoría para el seguimiento de la creación, actualización y eliminación lógica.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "branches")
public class Branch {

    /**
     * Identificador único de la sucursal.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_id")
    private Integer branchId;

    /**
     * Nombre de la sucursal.
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Dirección de la sucursal.
     */
    @Column(name = "address", length = 255)
    private String address;

    /**
     * Número de teléfono de la sucursal.
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * Prefijo para los números de orden generados por esta sucursal (ej. 'CDMX-', 'MTY-').
     * Debe ser único.
     */
    @Column(name = "order_prefix", nullable = false, unique = true, length = 10)
    private String orderPrefix;

    /**
     * Último número de secuencia utilizado para las órdenes de esta sucursal.
     */
    @Column(name = "last_order_sequence_number", nullable = false)
    private Integer lastOrderSequenceNumber = 0; // Valor por defecto según DDL

    /**
     * Indicador de borrado lógico. True si la sucursal está eliminada lógicamente.
     */
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false; // Valor por defecto según DDL

    /**
     * Fecha y hora de creación del registro. Se auto-genera al persistir.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * ID del usuario que creó el registro.
     */
    @Column(name = "created_by_user_id")
    private Integer createdByUserId;

    /**
     * Fecha y hora de la última actualización del registro. Se auto-actualiza.
     */
    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt;

    /**
     * ID del usuario que realizó la última actualización.
     */
    @Column(name = "updated_by_user_id")
    private Integer updatedByUserId;

    /**
     * Fecha y hora de la eliminación lógica del registro.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * ID del usuario que realizó la eliminación lógica.
     */
    @Column(name = "deleted_by_user_id")
    private Integer deletedByUserId;

    /**
     * Constructor para la creación inicial de una sucursal.
     * Los campos de auditoría se manejan mediante {@link PrePersist} y {@link PreUpdate}.
     *
     * @param name Nombre de la sucursal.
     * @param address Dirección de la sucursal.
     * @param phone Número de teléfono de la sucursal.
     * @param orderPrefix Prefijo para los números de orden.
     */
    public Branch(String name, String address, String phone, String orderPrefix) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.orderPrefix = orderPrefix;
        this.isDeleted = false;
        this.lastOrderSequenceNumber = 0;
    }

    /**
     * Método que se ejecuta antes de persistir la entidad por primera vez.
     * Establece {@code createdAt} y {@code lastUpdatedAt}.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /**
     * Método que se ejecuta antes de actualizar la entidad.
     * Actualiza {@code lastUpdatedAt}.
     */
    @PreUpdate
    protected void onUpdate() {
        this.lastUpdatedAt = LocalDateTime.now();
    }
}