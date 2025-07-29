package com.muebleria.mctecommercebackend.repository;

import com.muebleria.mctecommercebackend.model.Driver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad {@link Driver}.
 * Proporciona métodos para interactuar con la tabla 'drivers' en la base de datos.
 */
@Repository
public interface DriverRepository extends JpaRepository<Driver, Integer> {

    /**
     * Busca un chófer por su nombre, que no esté eliminado lógicamente.
     *
     * @param name El nombre del chófer a buscar.
     * @return Un {@link Optional} que contiene el chófer si se encuentra, o un Optional vacío.
     */
    Optional<Driver> findByNameAndIsDeletedFalse(String name);

    /**
     * Busca un chófer por su número de licencia, que no esté eliminado lógicamente.
     *
     * @param license El número de licencia del chófer a buscar.
     * @return Un {@link Optional} que contiene el chófer si se encuentra, o un Optional vacío.
     */
    Optional<Driver> findByLicenseAndIsDeletedFalse(String license);

    /**
     * Obtiene una página de chóferes activos (no eliminados lógicamente).
     *
     * @param pageable Objeto Pageable con la información de paginación.
     * @return Una Page de entidades Driver activas.
     */
    Page<Driver> findByIsDeletedFalse(Pageable pageable);

    /**
     * Verifica si ya existe un chófer con el mismo número de licencia
     * y que no esté eliminado lógicamente (útil para validación de unicidad en creación).
     *
     * @param license El número de licencia a verificar.
     * @return True si ya existe un chófer con esa licencia.
     */
    boolean existsByLicenseAndIsDeletedFalse(String license);

    /**
     * Verifica si ya existe un chófer con el mismo número de licencia,
     * excluyendo un ID específico (útil para validación de unicidad en actualización).
     *
     * @param license El número de licencia a verificar.
     * @param driverId El ID del chófer a excluir de la búsqueda.
     * @return True si existe otro chófer con esa licencia y no está eliminado.
     */
    boolean existsByLicenseAndDriverIdNotAndIsDeletedFalse(String license, Integer driverId);
}