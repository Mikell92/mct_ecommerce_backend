package com.muebleria.mctecommercebackend.repository;

import com.muebleria.mctecommercebackend.model.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    Optional<Branch> findByName(String name);

    Optional<Branch> findByOrderPrefix(String orderPrefix);

    Page<Branch> findByIsDeletedFalse(Pageable pageable);
}