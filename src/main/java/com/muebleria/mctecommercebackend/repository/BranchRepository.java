package com.muebleria.mctecommercebackend.repository;

import com.muebleria.mctecommercebackend.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad {@link Branch}.
 * Proporciona métodos para interactuar con la tabla 'branches' en la base de datos.
 */
@Repository
public interface BranchRepository extends JpaRepository<Branch, Integer> {

    /**
     * Busca una sucursal por su nombre.
     *
     * @param name El nombre de la sucursal a buscar.
     * @return Un {@link Optional} que contiene la sucursal si se encuentra, o un Optional vacío.
     */
    Optional<Branch> findByName(String name);

    /**
     * Busca una sucursal por su prefijo de orden.
     *
     * @param orderPrefix El prefijo de orden de la sucursal a buscar.
     * @return Un {@link Optional} que contiene la sucursal si se encuentra, o un Optional vacío.
     */
    Optional<Branch> findByOrderPrefix(String orderPrefix);
}