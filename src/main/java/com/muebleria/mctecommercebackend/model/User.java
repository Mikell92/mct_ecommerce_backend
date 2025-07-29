package com.muebleria.mctecommercebackend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity // Indica que esta clase es una entidad JPA y se mapeará a una tabla de BD
@Table(name = "users", // Especifica el nombre exacto de la tabla en la base de datos
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "username") // Asegura que el username sea único
        })
@Data // Genera automáticamente getters, setters, toString, equals y hashCode de Lombok
@NoArgsConstructor // Genera un constructor sin argumentos (necesario para JPA)
@AllArgsConstructor // Genera un constructor con todos los argumentos
public class User {
    @Id // Marca el campo como la clave primaria de la tabla
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Indica que el valor del ID es auto-incrementable por la base de datos
    @Column(name = "user_id") // Mapea el campo Java al nombre de la columna 'user_id' en la tabla
    private Integer userId; // Usamos Integer para INT de la DB

    @Column(name = "username", nullable = false, unique = true, length = 100) // Mapea 'username', no nulo, único, longitud máxima 100
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255) // Mapeo directo a VARCHAR en la DB
    private String passwordHash; // Almacenaremos el hash de la contraseña aquí

    @Column(name = "role", nullable = false, length = 50) // Mapea 'role', no nulo, longitud máxima 50
    private String role; // Ej. 'ADMIN', 'CAJERO', 'GESTOR_INVENTARIO'

    @Column(name = "is_deleted", nullable = false) // Mapea 'is_deleted', no nulo
    private Boolean isDeleted; // Indica si el usuario está marcado como eliminado (borrado lógico)

    @Column(name = "created_at", nullable = false, updatable = false) // Mapea 'created_at', no nulo, no actualizable vía JPA (gestionado por @PrePersist/BD)
    private LocalDateTime createdAt;

    @Column(name = "created_by_user_id") // Mapea 'created_by_user_id'
    private Integer createdByUserId; // ID del usuario que creó este registro

    @Column(name = "last_updated_at", nullable = false) // Mapea 'last_updated_at', no nulo
    private LocalDateTime lastUpdatedAt; // Fecha de la última actualización

    @Column(name = "updated_by_user_id") // Mapea 'updated_by_user_id'
    private Integer updatedByUserId; // ID del usuario que actualizó este registro

    @Column(name = "deleted_at") // Mapea 'deleted_at'
    private LocalDateTime deletedAt; // Fecha en que el registro fue marcado como eliminado

    @Column(name = "deleted_by_user_id") // Mapea 'deleted_by_user_id'
    private Integer deletedByUserId; // ID del usuario que marcó este registro como eliminado

    // --- NUEVA PROPIEDAD PARA ASOCIAR A LA SUCURSAL GESTIONADA ---
    @ManyToOne(fetch = FetchType.LAZY) // Lazy loading es recomendable
    @JoinColumn(name = "managed_branch_id", referencedColumnName = "branch_id")
    private Branch managedBranch; // Referencia a la entidad Branch (asegúrate de que exista Branch.java)


    // Constructor para registro de nuevos usuarios (simplificado para el servicio)
    public User(String username, String passwordHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.isDeleted = false; // Por defecto, no eliminado
        this.createdAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
    }

    // -- Métodos de ciclo de vida de JPA para gestionar timestamps (ejecutados automáticamente) --
    @PrePersist // Se ejecuta antes de que la entidad sea persistida (guardada por primera vez)
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.lastUpdatedAt == null) {
            this.lastUpdatedAt = LocalDateTime.now();
        }
        if (this.isDeleted == null) { // Asegura que isDeleted tenga un valor predeterminado si no se establece
            this.isDeleted = false;
        }
    }

    @PreUpdate // Se ejecuta antes de que la entidad sea actualizada
    protected void onUpdate() {
        this.lastUpdatedAt = LocalDateTime.now();
    }
}