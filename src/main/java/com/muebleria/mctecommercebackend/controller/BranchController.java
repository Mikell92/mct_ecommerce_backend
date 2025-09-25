package com.muebleria.mctecommercebackend.controller;

import com.muebleria.mctecommercebackend.dto.BranchDTO;
import com.muebleria.mctecommercebackend.dto.BranchListDTO;
import com.muebleria.mctecommercebackend.dto.BranchSummaryDTO;
import com.muebleria.mctecommercebackend.dto.BranchUpdateDTO;
import com.muebleria.mctecommercebackend.exception.ResourceNotFoundException;
import com.muebleria.mctecommercebackend.model.UserStatus;
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

import java.util.List;

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
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN', 'GESTOR_SUCURSAL')")
    public ResponseEntity<BranchDTO> getBranchById(@PathVariable Long id) {
        BranchDTO branchDTO = branchService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con ID: " + id));
        return ResponseEntity.ok(branchDTO);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN', 'GESTOR_SUCURSAL')")
    public ResponseEntity<Page<BranchListDTO>> getAllBranches(
            @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @RequestParam(defaultValue = "ACTIVE") UserStatus status,
            @RequestParam(required = false) String search) {
        Page<BranchListDTO> branches = branchService.findAll(pageable, status, search);
        return ResponseEntity.ok(branches);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN', 'GESTOR_SUCURSAL')")
    public ResponseEntity<List<BranchSummaryDTO>> getAllBranchSummaries() {
        List<BranchSummaryDTO> branches = branchService.findAllSummaries();
        return ResponseEntity.ok(branches);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<BranchDTO> updateBranch(@PathVariable Long id, @Valid @RequestBody BranchUpdateDTO branchUpdateDTO) {
        BranchDTO updatedBranch = branchService.updateBranch(id, branchUpdateDTO);
        return ResponseEntity.ok(updatedBranch);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<Void> deleteBranch(@PathVariable Long id) {
        branchService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('DEVELOPER')")
    public ResponseEntity<BranchDTO> restoreBranch(@PathVariable Long id) {
        BranchDTO restoredBranch = branchService.restoreBranchById(id);
        return ResponseEntity.ok(restoredBranch);
    }
}