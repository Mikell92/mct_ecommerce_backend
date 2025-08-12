package com.muebleria.mctecommercebackend.repository;

import com.muebleria.mctecommercebackend.model.Role;
import com.muebleria.mctecommercebackend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @EntityGraph(attributePaths = "managedBranch")
    Optional<User> findByUsername(String username);

    Page<User> findByIsDeletedFalse(Pageable pageable);

    Page<User> findByRoleNotAndIsDeletedFalse(Role role, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "accessRules")
    Optional<User> findById(Long id);
}