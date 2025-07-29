package com.muebleria.mctecommercebackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad que representa la tabla 'drivers' en la base de datos.
 * Almacena la información de los chóferes disponibles para la entrega de pedidos.
 * Incluye campos para auditoría y borrado lógico.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "drivers")
public class Driver {

    /**
     * Identificador único del chófer.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "driver_id")
    private Integer driverId;

    /**
     * Nombre completo del chófer.
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Número de teléfono de contacto del chófer.
     * Puede ser nulo.
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * Número de licencia de conducir del chófer.
     * Debe ser único si no es nulo.
     */
    @Column(name = "license", unique = true, length = 50)
    private String license;

    /**
     * Indica si el chófer está actualmente activo y disponible para asignaciones.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // Valor por defecto según DDL

    /**
     * Indicador de borrado lógico. True si el chófer está eliminado lógicamente.
     */
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false; // Valor por defecto según DDL

    // --- Campos de Auditoría ---
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by_user_id")
    private Integer createdByUserId;

    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt;

    @Column(name = "updated_by_user_id")
    private Integer updatedByUserId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by_user_id")
    private Integer deletedByUserId;

    /**
     * Constructor para la creación inicial de un chófer.
     * Los campos de auditoría y fechas se manejan mediante {@link PrePersist} y {@link PreUpdate}.
     *
     * @param name Nombre completo del chófer.
     * @param phone Número de teléfono de contacto.
     * @param license Número de licencia de conducir.
     */
    public Driver(String name, String phone, String license) {
        this.name = name;
        this.phone = phone;
        this.license = license;
        this.isActive = true; // Por defecto activo
        this.isDeleted = false; // Por defecto no eliminado
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