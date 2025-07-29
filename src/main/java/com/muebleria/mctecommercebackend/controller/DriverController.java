package com.muebleria.mctecommercebackend.controller;

import com.muebleria.mctecommercebackend.dto.DriverDTO;
import com.muebleria.mctecommercebackend.exception.ResourceNotFoundException;
import com.muebleria.mctecommercebackend.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controlador REST para la gestión de chóferes.
 * Expone endpoints para crear, leer, actualizar y eliminar (lógicamente) chóferes.
 * Requiere autenticación y autorización para la mayoría de las operaciones.
 */
@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;

    @Autowired
    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    /**
     * Crea un nuevo chófer.
     * Solo accesible por usuarios con rol ADMIN.
     *
     * @param driverDTO El DTO del chófer a crear.
     * @return ResponseEntity con el DTO del chófer creado y el estado HTTP 201 (Created).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DriverDTO> createDriver(@Valid @RequestBody DriverDTO driverDTO) {
        DriverDTO savedDriver = driverService.saveDriver(driverDTO);
        return new ResponseEntity<>(savedDriver, HttpStatus.CREATED);
    }

    /**
     * Obtiene un chófer por su ID.
     * Accesible por usuarios con rol ADMIN o GESTOR_INVENTARIO.
     *
     * @param id El ID del chófer a buscar.
     * @return ResponseEntity con el DTO del chófer y el estado HTTP 200 (OK),
     * o 404 (Not Found) si no existe o está eliminado lógicamente.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_INVENTARIO')")
    public ResponseEntity<DriverDTO> getDriverById(@PathVariable Integer id) {
        Optional<DriverDTO> driverDTO = driverService.findById(id);
        return driverDTO.map(dto -> new ResponseEntity<>(dto, HttpStatus.OK))
                .orElseThrow(() -> new ResourceNotFoundException("Chófer no encontrado con ID: " + id));
    }

    /**
     * Obtiene un chófer por su número de licencia.
     * Accesible por usuarios con rol ADMIN o GESTOR_INVENTARIO.
     *
     * @param license El número de licencia del chófer a buscar.
     * @return ResponseEntity con el DTO del chófer y el estado HTTP 200 (OK),
     * o 404 (Not Found) si no existe o está eliminado lógicamente.
     */
    @GetMapping("/byLicense/{license}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_INVENTARIO')")
    public ResponseEntity<DriverDTO> getDriverByLicense(@PathVariable String license) {
        Optional<DriverDTO> driverDTO = driverService.findByLicense(license);
        return driverDTO.map(dto -> new ResponseEntity<>(dto, HttpStatus.OK))
                .orElseThrow(() -> new ResourceNotFoundException("Chófer no encontrado con licencia: " + license));
    }

    /**
     * Obtiene una lista paginada de todos los chóferes activos.
     * Accesible por usuarios con rol ADMIN o GESTOR_INVENTARIO.
     *
     * @param pageable Objeto Pageable para la paginación (ej. ?page=0&size=10&sort=name,asc).
     * @return ResponseEntity con una página de DTOs de chóferes y el estado HTTP 200 (OK).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_INVENTARIO')")
    public ResponseEntity<Page<DriverDTO>> getAllActiveDrivers(@PageableDefault(size = 10, sort = "name") Pageable pageable) {
        Page<DriverDTO> drivers = driverService.findAllActiveDrivers(pageable);
        return new ResponseEntity<>(drivers, HttpStatus.OK);
    }

    /**
     * Actualiza un chófer existente.
     * Solo accesible por usuarios con rol ADMIN.
     *
     * @param id El ID del chófer a actualizar.
     * @param driverDTO El DTO con los datos actualizados del chófer.
     * @return ResponseEntity con el DTO del chófer actualizado y el estado HTTP 200 (OK).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DriverDTO> updateDriver(@PathVariable Integer id, @Valid @RequestBody DriverDTO driverDTO) {
        DriverDTO updatedDriver = driverService.updateDriver(id, driverDTO);
        return new ResponseEntity<>(updatedDriver, HttpStatus.OK);
    }

    /**
     * Realiza el borrado lógico de un chófer.
     * Solo accesible por usuarios con rol ADMIN.
     *
     * @param id El ID del chófer a eliminar lógicamente.
     * @return ResponseEntity con el estado HTTP 204 (No Content) si la operación fue exitosa.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDriver(@PathVariable Integer id) {
        driverService.deleteDriver(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}