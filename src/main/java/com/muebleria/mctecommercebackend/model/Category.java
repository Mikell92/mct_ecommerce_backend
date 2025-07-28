package com.muebleria.mctecommercebackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad que representa la tabla 'categories' en la base de datos.
 * <p>
 * Incluye campos para auditoría (quién y cuándo se creó, actualizó o eliminó)
 * y para el borrado lógico (`is_deleted`).
 * </p>
 */
@Entity
@Table(name = "categories")
@Data // Genera getters, setters, toString, equals y hashCode
@NoArgsConstructor // Genera un constructor sin argumentos
@AllArgsConstructor // Genera un constructor con todos los argumentos
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "name", nullable = false, unique = true) // Validación de unicidad a nivel de DB
    private String name;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false; // Implementa el borrado lógico

    @Column(name = "created_at", updatable = false, nullable = false) // Asegúrate de que nullable sea false
    private LocalDateTime createdAt;

    @Column(name = "created_by_user_id")
    private Integer createdByUserId;

    @Column(name = "last_updated_at", nullable = false) // Asegúrate de que nullable sea false
    private LocalDateTime lastUpdatedAt;

    @Column(name = "updated_by_user_id")
    private Integer updatedByUserId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by_user_id")
    private Integer deletedByUserId;

    /**
     * Callback de JPA que asigna la fecha y hora de creación antes de guardar.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        // **Añade esta línea para inicializar lastUpdatedAt en la creación**
        this.lastUpdatedAt = LocalDateTime.now();
        if (this.isDeleted == null) { // Asegura que isDeleted también tenga un valor
            this.isDeleted = false;
        }
    }

    /**
     * Callback de JPA que asigna la fecha y hora de la última actualización antes de actualizar.
     */
    @PreUpdate
    protected void onUpdate() {
        this.lastUpdatedAt = LocalDateTime.now();
    }
}