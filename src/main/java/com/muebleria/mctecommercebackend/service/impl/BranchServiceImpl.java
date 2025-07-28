package com.muebleria.mctecommercebackend.service.impl;

import com.muebleria.mctecommercebackend.dto.BranchDTO;
import com.muebleria.mctecommercebackend.exception.ResourceNotFoundException;
import com.muebleria.mctecommercebackend.model.Branch;
import com.muebleria.mctecommercebackend.repository.BranchRepository;
import com.muebleria.mctecommercebackend.security.user.UserDetailsImpl;
import com.muebleria.mctecommercebackend.service.BranchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación de la interfaz {@link BranchService} para la gestión de sucursales.
 * <p>
 * Contiene la lógica de negocio, incluyendo validaciones de unicidad, gestión de auditoría
 * y borrado lógico para las operaciones CRUD de sucursales.
 * </p>
 */
@Service
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;

    @Autowired
    public BranchServiceImpl(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    /**
     * Método auxiliar para obtener el ID del usuario actualmente autenticado.
     * Es crucial para la auditoría automática.
     *
     * @return Un {@link Optional} que contiene el ID del usuario si está autenticado,
     * o un Optional vacío si no hay un usuario autenticado o el principal no es {@link UserDetailsImpl}.
     */
    private Optional<Integer> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return Optional.of(userDetails.getId());
        }
        return Optional.empty();
    }

    /**
     * Guarda una nueva sucursal en la base de datos.
     * Realiza validación de unicidad del nombre y el prefijo de orden, y puebla los campos de auditoría.
     *
     * @param branchDTO El DTO con la información de la nueva sucursal.
     * @return El DTO de la sucursal guardada.
     * @throws RuntimeException si el nombre o el prefijo de la sucursal ya existen.
     */
    @Override
    public BranchDTO saveBranch(BranchDTO branchDTO) {
        // Validar unicidad del nombre
        if (branchRepository.findByName(branchDTO.getName()).isPresent()) {
            throw new RuntimeException("El nombre de la sucursal '" + branchDTO.getName() + "' ya existe.");
        }
        // Validar unicidad del prefijo de orden
        if (branchRepository.findByOrderPrefix(branchDTO.getOrderPrefix()).isPresent()) {
            throw new RuntimeException("El prefijo de orden '" + branchDTO.getOrderPrefix() + "' ya está en uso por otra sucursal.");
        }

        Branch branch = new Branch();
        branch.setName(branchDTO.getName());
        branch.setAddress(branchDTO.getAddress());
        branch.setPhone(branchDTO.getPhone());
        branch.setOrderPrefix(branchDTO.getOrderPrefix());
        branch.setIsDeleted(false); // Por defecto, una nueva sucursal no está eliminada
        branch.setLastOrderSequenceNumber(0); // Se inicializa en 0 según el DDL

        // Asignar el ID del usuario que la crea (Auditoría)
        getCurrentUserId().ifPresent(branch::setCreatedByUserId);

        Branch savedBranch = branchRepository.save(branch);
        return BranchDTO.fromEntity(savedBranch);
    }

    /**
     * Busca una sucursal por su ID.
     *
     * @param id El ID de la sucursal.
     * @return Un {@link Optional} que contiene el DTO de la sucursal si existe y no está
     * eliminada lógicamente, o un Optional vacío en caso contrario.
     */
    @Override
    public Optional<BranchDTO> findById(Integer id) {
        return branchRepository.findById(id)
                .filter(branch -> !branch.getIsDeleted()) // Solo devuelve si no está borrada lógicamente
                .map(BranchDTO::fromEntity); // Convierte la entidad a DTO
    }

    /**
     * Busca una sucursal por su nombre.
     *
     * @param name El nombre de la sucursal.
     * @return Un {@link Optional} que contiene el DTO de la sucursal si existe y no está
     * eliminada lógicamente, o un Optional vacío en caso contrario.
     */
    @Override
    public Optional<BranchDTO> findByName(String name) {
        return branchRepository.findByName(name)
                .filter(branch -> !branch.getIsDeleted()) // Solo devuelve si no está borrada lógicamente
                .map(BranchDTO::fromEntity); // Convierte la entidad a DTO
    }

    /**
     * Obtiene una página de sucursales activas (no eliminadas lógicamente).
     *
     * @param pageable Objeto Pageable con la información de paginación.
     * @return Una Page de DTOs de sucursales activas.
     */
    @Override
    public Page<BranchDTO> findAllActiveBranches(Pageable pageable) {
        List<Branch> activeBranches = branchRepository.findAll().stream()
                .filter(branch -> !branch.getIsDeleted())
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), activeBranches.size());
        List<BranchDTO> pageContent = activeBranches.subList(start, end)
                .stream()
                .map(BranchDTO::fromEntity)
                .collect(Collectors.toList());

        return new PageImpl<>(pageContent, pageable, activeBranches.size());
    }

    /**
     * Actualiza una sucursal existente.
     *
     * @param id El ID de la sucursal a actualizar.
     * @param branchDTO El DTO con los datos actualizados.
     * @return El DTO de la sucursal actualizada.
     * @throws ResourceNotFoundException si la sucursal no se encuentra o ya está eliminada lógicamente.
     * @throws RuntimeException si el nuevo nombre o prefijo de orden ya está en uso por otra sucursal.
     */
    @Override
    public BranchDTO updateBranch(Integer id, BranchDTO branchDTO) {
        Branch existingBranch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con ID: " + id));

        // No se debe permitir actualizar una sucursal que ya ha sido borrada lógicamente
        if (existingBranch.getIsDeleted()) {
            throw new ResourceNotFoundException("Sucursal no encontrada con ID: " + id);
        }

        // Valida que el nuevo nombre no exista si se está cambiando
        if (!existingBranch.getName().equalsIgnoreCase(branchDTO.getName())) {
            if (branchRepository.findByName(branchDTO.getName()).isPresent()) {
                throw new RuntimeException("El nuevo nombre de la sucursal '" + branchDTO.getName() + "' ya existe.");
            }
            existingBranch.setName(branchDTO.getName());
        }

        // Valida que el nuevo prefijo de orden no exista si se está cambiando
        if (!existingBranch.getOrderPrefix().equalsIgnoreCase(branchDTO.getOrderPrefix())) {
            if (branchRepository.findByOrderPrefix(branchDTO.getOrderPrefix()).isPresent()) {
                throw new RuntimeException("El nuevo prefijo de orden '" + branchDTO.getOrderPrefix() + "' ya está en uso por otra sucursal.");
            }
            existingBranch.setOrderPrefix(branchDTO.getOrderPrefix());
        }

        existingBranch.setAddress(branchDTO.getAddress());
        existingBranch.setPhone(branchDTO.getPhone());
        // lastOrderSequenceNumber no se actualiza a través de este método, se gestiona internamente

        // Auditoría: Asigna el ID del usuario que la actualiza
        getCurrentUserId().ifPresent(existingBranch::setUpdatedByUserId);

        Branch updatedBranch = branchRepository.save(existingBranch);
        return BranchDTO.fromEntity(updatedBranch);
    }

    /**
     * Realiza el borrado lógico de una sucursal.
     * En lugar de borrar el registro, marca el campo 'is_deleted' como true
     * y registra la fecha y el usuario que realizó la acción.
     *
     * @param id El ID de la sucursal a eliminar lógicamente.
     * @throws ResourceNotFoundException si la sucursal no se encuentra.
     */
    @Override
    public void deleteBranch(Integer id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con ID: " + id));

        // Marcar como eliminado y registrar auditoría
        branch.setIsDeleted(true);
        branch.setDeletedAt(LocalDateTime.now());
        getCurrentUserId().ifPresent(branch::setDeletedByUserId);

        branchRepository.save(branch);
    }
}