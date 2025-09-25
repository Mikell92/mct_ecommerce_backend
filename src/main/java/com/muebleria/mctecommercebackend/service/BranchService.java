package com.muebleria.mctecommercebackend.service;

import com.muebleria.mctecommercebackend.dto.BranchDTO;
import com.muebleria.mctecommercebackend.dto.BranchListDTO;
import com.muebleria.mctecommercebackend.dto.BranchSummaryDTO;
import com.muebleria.mctecommercebackend.dto.BranchUpdateDTO;
import com.muebleria.mctecommercebackend.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface BranchService {

    BranchDTO createBranch(BranchDTO branchDTO);

    Optional<BranchDTO> findById(Long id);

    Page<BranchListDTO> findAll(Pageable pageable, UserStatus status, String search);

    List<BranchSummaryDTO> findAllSummaries();

    BranchDTO updateBranch(Long id, BranchUpdateDTO branchUpdateDTO);

    void deleteById(Long id);

    BranchDTO restoreBranchById(Long id);
}