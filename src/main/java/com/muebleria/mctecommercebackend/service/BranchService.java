package com.muebleria.mctecommercebackend.service;

import com.muebleria.mctecommercebackend.dto.BranchDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Interfaz de servicio para la gestión de sucursales.
 * Define las operaciones de negocio para la creación, lectura, actualización y eliminación
 * lógica de sucursales, así como la obtención de listas paginadas.
 */
public interface BranchService {

    /**
     * Guarda una nueva sucursal en la base de datos.
     *
     * @param branchDTO El DTO con la información de la nueva sucursal.
     * @return El DTO de la sucursal guardada.
     */
    BranchDTO saveBranch(BranchDTO branchDTO);

    /**
     * Busca una sucursal por su ID. Solo devuelve sucursales no eliminadas lógicamente.
     *
     * @param id El ID de la sucursal.
     * @return Un {@link Optional} que contiene el DTO de la sucursal si se encuentra y no está eliminada,
     * o un Optional vacío.
     */
    Optional<BranchDTO> findById(Integer id);

    /**
     * Busca una sucursal por su nombre. Solo devuelve sucursales no eliminadas lógicamente.
     *
     * @param name El nombre de la sucursal.
     * @return Un {@link Optional} que contiene el DTO de la sucursal si se encuentra y no está eliminada,
     * o un Optional vacío.
     */
    Optional<BranchDTO> findByName(String name);

    /**
     * Obtiene una página de sucursales activas (no eliminadas lógicamente).
     *
     * @param pageable Objeto Pageable con la información de paginación.
     * @return Una Page de DTOs de sucursales activas.
     */

    Page<BranchDTO> findAllActiveBranches(Pageable pageable);

    /**
     * Actualiza una sucursal existente.
     *
     * @param id El ID de la sucursal a actualizar.
     * @param branchDTO El DTO con los datos actualizados.
     * @return El DTO de la sucursal actualizada.
     */
    BranchDTO updateBranch(Integer id, BranchDTO branchDTO);

    /**
     * Realiza el borrado lógico de una sucursal.
     *
     * @param id El ID de la sucursal a eliminar lógicamente.
     */
    void deleteBranch(Integer id);
}