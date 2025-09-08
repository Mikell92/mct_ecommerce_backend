package com.muebleria.mctecommercebackend.service;

import com.muebleria.mctecommercebackend.dto.BranchDTO;
import com.muebleria.mctecommercebackend.dto.BranchSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface BranchService {

    BranchDTO createBranch(BranchDTO branchDTO);

    Optional<BranchDTO> findById(Long id);

    Page<BranchDTO> findAll(Pageable pageable);

    List<BranchSummaryDTO> findAllSummaries();

    BranchDTO updateBranch(Long id, BranchDTO branchDTO);

    void deleteById(Long id);
}