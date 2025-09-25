package com.muebleria.mctecommercebackend.service.impl;

import com.muebleria.mctecommercebackend.dto.BranchDTO;
import com.muebleria.mctecommercebackend.dto.BranchListDTO;
import com.muebleria.mctecommercebackend.dto.BranchSummaryDTO;
import com.muebleria.mctecommercebackend.dto.BranchUpdateDTO;
import com.muebleria.mctecommercebackend.exception.ResourceNotFoundException;
import com.muebleria.mctecommercebackend.model.Branch;
import com.muebleria.mctecommercebackend.model.Role;
import com.muebleria.mctecommercebackend.model.User;
import com.muebleria.mctecommercebackend.model.UserStatus;
import com.muebleria.mctecommercebackend.repository.BranchRepository;
import com.muebleria.mctecommercebackend.repository.UserRepository;
import com.muebleria.mctecommercebackend.security.user.UserDetailsImpl;
import com.muebleria.mctecommercebackend.service.BranchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final UserRepository userRepository;

    @Autowired
    public BranchServiceImpl(BranchRepository branchRepository, UserRepository userRepository) {
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public BranchDTO createBranch(BranchDTO branchDTO) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));

        branchRepository.findByName(branchDTO.getName()).ifPresent(b -> {
            throw new RuntimeException("El nombre de la sucursal '" + branchDTO.getName() + "' ya existe.");
        });
        branchRepository.findByOrderPrefix(branchDTO.getOrderPrefix()).ifPresent(b -> {
            throw new RuntimeException("El prefijo '" + branchDTO.getOrderPrefix() + "' ya está en uso.");
        });

        Branch branch = new Branch();
        mapDtoToEntity(branchDTO, branch);

        branch.setCreatedBy(currentUser);
        branch.setUpdatedBy(currentUser);

        Branch savedBranch = branchRepository.save(branch);
        return toDTO(savedBranch);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BranchDTO> findById(Long id) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));

        if (currentUser.getRole() == Role.DEVELOPER) {
            return branchRepository.findByIdIncludingDeleted(id).map(this::toDTO);
        }

        return branchRepository.findById(id).map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BranchListDTO> findAll(Pageable pageable, UserStatus status, String search) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));

        if (status == UserStatus.DELETED && currentUser.getRole() != Role.DEVELOPER) {
            throw new AccessDeniedException("No tienes permiso para ver la lista de sucursales eliminadas.");
        }

        Specification<Branch> spec = (root, query, cb) -> cb.conjunction();

        if (status == UserStatus.DELETED) {
            spec = spec.and((root, query, cb) -> cb.isTrue(root.get("isDeleted")));
        } else {
            // Para cualquier otro caso (ACTIVE, ALL, o nulo), solo mostramos los no eliminados.
            spec = spec.and((root, query, cb) -> cb.isFalse(root.get("isDeleted")));
        }

        if (search != null && !search.trim().isEmpty()) {
            String searchTerm = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), searchTerm),
                    cb.like(cb.lower(root.get("neighborhood")), searchTerm),
                    cb.like(cb.lower(root.get("city")), searchTerm),
                    cb.like(cb.lower(root.get("state")), searchTerm)
            ));
        }

        return branchRepository.findAll(spec, pageable).map(this::toBranchListDTO);
    }

    private BranchListDTO toBranchListDTO(Branch branch) {
        BranchListDTO dto = new BranchListDTO();
        dto.setId(branch.getId());
        dto.setName(branch.getName());
        dto.setNeighborhood(branch.getNeighborhood());
        dto.setCity(branch.getCity());
        dto.setState(branch.getState());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchSummaryDTO> findAllSummaries() {
        return branchRepository.findByIsDeletedFalse().stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BranchDTO updateBranch(Long id, BranchUpdateDTO branchUpdateDTO) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));

        Branch branch = branchRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con ID: " + id));

        if (branch.isDeleted()) {
            throw new AccessDeniedException("No se puede modificar una sucursal que ha sido eliminada.");
        }

        if (branchUpdateDTO.getName() != null) {
            branchRepository.findByName(branchUpdateDTO.getName()).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new RuntimeException("El nombre '" + branchUpdateDTO.getName() + "' ya pertenece a otra sucursal.");
                }
            });
            branch.setName(branchUpdateDTO.getName());
        }

        if (branchUpdateDTO.getOrderPrefix() != null) {
            branchRepository.findByOrderPrefix(branchUpdateDTO.getOrderPrefix()).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new RuntimeException("El prefijo '" + branchUpdateDTO.getOrderPrefix() + "' ya está en uso.");
                }
            });
            branch.setOrderPrefix(branchUpdateDTO.getOrderPrefix());
        }

        if (branchUpdateDTO.getStreetAddress() != null) branch.setStreetAddress(branchUpdateDTO.getStreetAddress());
        if (branchUpdateDTO.getAddressLine2() != null) branch.setAddressLine2(branchUpdateDTO.getAddressLine2());
        if (branchUpdateDTO.getNeighborhood() != null) branch.setNeighborhood(branchUpdateDTO.getNeighborhood());
        if (branchUpdateDTO.getCity() != null) branch.setCity(branchUpdateDTO.getCity());
        if (branchUpdateDTO.getState() != null) branch.setState(branchUpdateDTO.getState());
        if (branchUpdateDTO.getPostalCode() != null) branch.setPostalCode(branchUpdateDTO.getPostalCode());
        if (branchUpdateDTO.getPhone() != null) branch.setPhone(branchUpdateDTO.getPhone());
        if (branchUpdateDTO.getRfc() != null) branch.setRfc(branchUpdateDTO.getRfc());

        branch.setUpdatedBy(currentUser);
        Branch updatedBranch = branchRepository.save(branch);
        return toDTO(updatedBranch);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));
        // Usamos el nuevo método para asegurar que no se intente borrar algo ya borrado.
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con ID: " + id));

        branch.setDeleted(true);
        branch.setDeletedAt(LocalDateTime.now());
        branch.setDeletedBy(currentUser);
        branchRepository.save(branch);
    }

    @Override
    @Transactional
    public BranchDTO restoreBranchById(Long id) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));
        // Usamos el nuevo método para encontrar la sucursal aunque esté eliminada.
        Branch branch = branchRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con ID: " + id));

        if (!branch.isDeleted()) {
            throw new IllegalArgumentException("La sucursal no está eliminada y no puede ser restaurada.");
        }

        branch.setDeleted(false);
        branch.setDeletedAt(null);
        branch.setDeletedBy(null);
        branch.setUpdatedBy(currentUser);

        Branch restoredBranch = branchRepository.save(branch);
        return toDTO(restoredBranch);
    }

    private BranchSummaryDTO toSummaryDTO(Branch branch) {
        return new BranchSummaryDTO(branch.getId(), branch.getName());
    }

    private BranchDTO toDTO(Branch branch) {
        BranchDTO dto = new BranchDTO();
        dto.setId(branch.getId());
        dto.setName(branch.getName());
        dto.setStreetAddress(branch.getStreetAddress());
        dto.setAddressLine2(branch.getAddressLine2());
        dto.setNeighborhood(branch.getNeighborhood());
        dto.setCity(branch.getCity());
        dto.setState(branch.getState());
        dto.setPostalCode(branch.getPostalCode());
        dto.setPhone(branch.getPhone());
        dto.setRfc(branch.getRfc());
        dto.setOrderPrefix(branch.getOrderPrefix());
        dto.setLastOrderSequenceNumber(branch.getLastOrderSequenceNumber());

        dto.setCreatedAt(branch.getCreatedAt());
        dto.setLastUpdatedAt(branch.getLastUpdatedAt());

        if (branch.getCreatedBy() != null && branch.getCreatedBy().getProfile() != null) {
            String fullName = branch.getCreatedBy().getProfile().getFirstName() + " " + branch.getCreatedBy().getProfile().getLastName();
            dto.setCreatedByFullName(fullName);
        }

        if (branch.getUpdatedBy() != null && branch.getUpdatedBy().getProfile() != null) {
            String fullName = branch.getUpdatedBy().getProfile().getFirstName() + " " + branch.getUpdatedBy().getProfile().getLastName();
            dto.setUpdatedByFullName(fullName);
        }

        if (branch.isDeleted()) {
            dto.setDeletedAt(branch.getDeletedAt());
            if (branch.getDeletedBy() != null && branch.getDeletedBy().getProfile() != null) {
                String fullName = branch.getDeletedBy().getProfile().getFirstName() + " " + branch.getDeletedBy().getProfile().getLastName();
                dto.setDeletedByFullName(fullName);
            }
        }

        return dto;
    }

    private void mapDtoToEntity(BranchDTO dto, Branch entity) {
        entity.setName(dto.getName());
        entity.setStreetAddress(dto.getStreetAddress());
        entity.setAddressLine2(dto.getAddressLine2());
        entity.setNeighborhood(dto.getNeighborhood());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setPostalCode(dto.getPostalCode());
        entity.setPhone(dto.getPhone());
        entity.setRfc(dto.getRfc());
        entity.setOrderPrefix(dto.getOrderPrefix());
        entity.setLastOrderSequenceNumber(dto.getLastOrderSequenceNumber());
    }

    private Optional<User> getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return Optional.empty();
        }
        Long userId = ((UserDetailsImpl) authentication.getPrincipal()).getId();
        return userRepository.findById(userId);
    }
}