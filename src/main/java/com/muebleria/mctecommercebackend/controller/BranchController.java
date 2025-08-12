package com.muebleria.mctecommercebackend.controller;

import com.muebleria.mctecommercebackend.dto.BranchDTO;
import com.muebleria.mctecommercebackend.exception.ResourceNotFoundException;
import com.muebleria.mctecommercebackend.service.BranchService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService branchService;

    @Autowired
    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<BranchDTO> createBranch(@Valid @RequestBody BranchDTO branchDTO) {
        BranchDTO savedBranch = branchService.createBranch(branchDTO);
        return new ResponseEntity<>(savedBranch, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN', 'GESTOR_SUCURSAL', 'GESTOR_INVENTARIO')")
    public ResponseEntity<BranchDTO> getBranchById(@PathVariable Long id) {
        BranchDTO branchDTO = branchService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con ID: " + id));
        return ResponseEntity.ok(branchDTO);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN', 'GESTOR_SUCURSAL', 'GESTOR_INVENTARIO')")
    public ResponseEntity<Page<BranchDTO>> getAllBranches(@PageableDefault(size = 10, sort = "name") Pageable pageable) {
        Page<BranchDTO> branches = branchService.findAll(pageable);
        return ResponseEntity.ok(branches);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<BranchDTO> updateBranch(@PathVariable Long id, @Valid @RequestBody BranchDTO branchDTO) {
        BranchDTO updatedBranch = branchService.updateBranch(id, branchDTO);
        return ResponseEntity.ok(updatedBranch);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<Void> deleteBranch(@PathVariable Long id) {
        branchService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}