package com.muebleria.mctecommercebackend.controller;

import com.muebleria.mctecommercebackend.dto.BranchDTO;
import com.muebleria.mctecommercebackend.exception.ResourceNotFoundException;
import com.muebleria.mctecommercebackend.service.BranchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controlador REST para la gestión de sucursales.
 * Expone endpoints para crear, leer, actualizar y eliminar (lógicamente) sucursales.
 * Requiere autenticación y autorización para la mayoría de las operaciones.
 */
@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService branchService;

    @Autowired
    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    /**
     * Crea una nueva sucursal.
     * Solo accesible por usuarios con rol ADMIN.
     *
     * @param branchDTO El DTO de la sucursal a crear.
     * @return ResponseEntity con el DTO de la sucursal creada y el estado HTTP 201 (Created).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BranchDTO> createBranch(@RequestBody BranchDTO branchDTO) {
        BranchDTO savedBranch = branchService.saveBranch(branchDTO);
        return new ResponseEntity<>(savedBranch, HttpStatus.CREATED);
    }

    /**
     * Obtiene una sucursal por su ID.
     * Accesible por usuarios con rol ADMIN o GESTOR_INVENTARIO.
     *
     * @param id El ID de la sucursal a buscar.
     * @return ResponseEntity con el DTO de la sucursal y el estado HTTP 200 (OK),
     * o 404 (Not Found) si no existe.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_INVENTARIO')")
    public ResponseEntity<BranchDTO> getBranchById(@PathVariable Integer id) {
        Optional<BranchDTO> branchDTO = branchService.findById(id);
        return branchDTO.map(dto -> new ResponseEntity<>(dto, HttpStatus.OK))
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con ID: " + id));
    }

    /**
     * Obtiene una sucursal por su nombre.
     * Accesible por usuarios con rol ADMIN o GESTOR_INVENTARIO.
     *
     * @param name El nombre de la sucursal a buscar.
     * @return ResponseEntity con el DTO de la sucursal y el estado HTTP 200 (OK),
     * o 404 (Not Found) si no existe.
     */
    @GetMapping("/byName/{name}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_INVENTARIO')")
    public ResponseEntity<BranchDTO> getBranchByName(@PathVariable String name) {
        Optional<BranchDTO> branchDTO = branchService.findByName(name);
        return branchDTO.map(dto -> new ResponseEntity<>(dto, HttpStatus.OK))
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con nombre: " + name));
    }

    /**
     * Obtiene una lista paginada de todas las sucursales activas.
     * Accesible por usuarios con rol ADMIN o GESTOR_INVENTARIO.
     *
     * @param pageable Objeto Pageable para la paginación (ej. ?page=0&size=10&sort=name,asc).
     * @return ResponseEntity con una página de DTOs de sucursales y el estado HTTP 200 (OK).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_INVENTARIO')")
    public ResponseEntity<Page<BranchDTO>> getAllActiveBranches(Pageable pageable) {
        Page<BranchDTO> branches = branchService.findAllActiveBranches(pageable);
        return new ResponseEntity<>(branches, HttpStatus.OK);
    }

    /**
     * Actualiza una sucursal existente.
     * Solo accesible por usuarios con rol ADMIN.
     *
     * @param id El ID de la sucursal a actualizar.
     * @param branchDTO El DTO con los datos actualizados de la sucursal.
     * @return ResponseEntity con el DTO de la sucursal actualizada y el estado HTTP 200 (OK).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BranchDTO> updateBranch(@PathVariable Integer id, @RequestBody BranchDTO branchDTO) {
        BranchDTO updatedBranch = branchService.updateBranch(id, branchDTO);
        return new ResponseEntity<>(updatedBranch, HttpStatus.OK);
    }

    /**
     * Realiza el borrado lógico de una sucursal.
     * Solo accesible por usuarios con rol ADMIN.
     *
     * @param id El ID de la sucursal a eliminar lógicamente.
     * @return ResponseEntity con el estado HTTP 204 (No Content) si la operación fue exitosa.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBranch(@PathVariable Integer id) {
        branchService.deleteBranch(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}