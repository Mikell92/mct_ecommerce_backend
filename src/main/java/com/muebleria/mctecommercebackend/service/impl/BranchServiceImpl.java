package com.muebleria.mctecommercebackend.service.impl;

import com.muebleria.mctecommercebackend.dto.BranchDTO;
import com.muebleria.mctecommercebackend.exception.ResourceNotFoundException;
import com.muebleria.mctecommercebackend.model.Branch;
import com.muebleria.mctecommercebackend.model.User;
import com.muebleria.mctecommercebackend.repository.BranchRepository;
import com.muebleria.mctecommercebackend.repository.UserRepository;
import com.muebleria.mctecommercebackend.security.user.UserDetailsImpl;
import com.muebleria.mctecommercebackend.service.BranchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

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
        return branchRepository.findById(id).map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BranchDTO> findAll(Pageable pageable) {
        return branchRepository.findAll(pageable).map(this::toDTO);
    }

    @Override
    @Transactional
    public BranchDTO updateBranch(Long id, BranchDTO branchDTO) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con ID: " + id));

        branchRepository.findByName(branchDTO.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new RuntimeException("El nombre '" + branchDTO.getName() + "' ya existe.");
            }
        });

        mapDtoToEntity(branchDTO, branch);
        branch.setUpdatedBy(currentUser);

        Branch updatedBranch = branchRepository.save(branch);
        return toDTO(updatedBranch);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        User currentUser = getCurrentUserEntity().orElseThrow(() -> new IllegalStateException("Usuario actual no identificado."));
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con ID: " + id));

        branch.setDeletedBy(currentUser);
        branchRepository.delete(branch);
    }

    private BranchDTO toDTO(Branch branch) {
        return new BranchDTO(
                branch.getId(),
                branch.getName(),
                branch.getStreetAddress(),
                branch.getAddressLine2(),
                branch.getNeighborhood(),
                branch.getCity(),
                branch.getState(),
                branch.getPostalCode(),
                branch.getPhone(),
                branch.getRfc(),
                branch.getOrderPrefix(),
                branch.getLastOrderSequenceNumber()
        );
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