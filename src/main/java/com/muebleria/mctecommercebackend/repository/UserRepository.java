package com.muebleria.mctecommercebackend.repository;

import com.muebleria.mctecommercebackend.model.Role;
import com.muebleria.mctecommercebackend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @EntityGraph(attributePaths = "managedBranch")
    Optional<User> findByUsername(String username);

    Page<User> findByIsDeletedFalse(Pageable pageable);

    Page<User> findByRoleNotAndIsDeletedFalse(Role role, Pageable pageable);

    @Override
    @Query(
            value = "SELECT * FROM users WHERE user_id = :id",
            nativeQuery = true
    )
    Optional<User> findById(Long id);

    /**
     * Busca una página de usuarios eliminados lógicamente usando una consulta nativa simple.
     * La ordenación compleja se manejará en el controlador.
     */
    @Query(
            value = "SELECT * FROM users WHERE is_deleted = true",
            countQuery = "SELECT count(*) FROM users WHERE is_deleted = true",
            nativeQuery = true
    )
    Page<User> findAllDeleted(Pageable pageable);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.accessRules WHERE u.id = :id")
    Optional<User> findByIdWithAccessRules(Long id);
}