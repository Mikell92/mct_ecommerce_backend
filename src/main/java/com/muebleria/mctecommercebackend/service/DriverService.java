package com.muebleria.mctecommercebackend.service;

import com.muebleria.mctecommercebackend.dto.DriverDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Interfaz de servicio para la gestión de chóferes.
 * Define las operaciones de negocio para la creación, lectura, actualización y eliminación
 * lógica de chóferes, así como la obtención de listas paginadas.
 */
public interface DriverService {

    /**
     * Guarda un nuevo chófer en la base de datos.
     *
     * @param driverDTO El DTO con la información del nuevo chófer.
     * @return El DTO del chófer guardado.
     */
    DriverDTO saveDriver(DriverDTO driverDTO);

    /**
     * Busca un chófer por su ID. Solo devuelve chóferes no eliminados lógicamente.
     *
     * @param id El ID del chófer.
     * @return Un {@link Optional} que contiene el DTO del chófer si se encuentra y no está eliminado,
     * o un Optional vacío.
     */
    Optional<DriverDTO> findById(Integer id);

    /**
     * Busca un chófer por su número de licencia. Solo devuelve chóferes no eliminados lógicamente.
     *
     * @param license El número de licencia del chófer.
     * @return Un {@link Optional} que contiene el DTO del chófer si se encuentra y no está eliminado,
     * o un Optional vacío.
     */
    Optional<DriverDTO> findByLicense(String license);

    /**
     * Obtiene una página de chóferes activos (no eliminados lógicamente).
     *
     * @param pageable Objeto Pageable con la información de paginación.
     * @return Una Page de DTOs de chóferes activos.
     */
    Page<DriverDTO> findAllActiveDrivers(Pageable pageable);

    /**
     * Actualiza un chófer existente.
     *
     * @param id El ID del chófer a actualizar.
     * @param driverDTO El DTO con los datos actualizados.
     * @return El DTO del chófer actualizado.
     */
    DriverDTO updateDriver(Integer id, DriverDTO driverDTO);

    /**
     * Realiza el borrado lógico de un chófer.
     *
     * @param id El ID del chófer a eliminar lógicamente.
     */
    void deleteDriver(Integer id);
}