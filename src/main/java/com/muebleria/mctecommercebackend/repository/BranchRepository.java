package com.muebleria.mctecommercebackend.repository;

import com.muebleria.mctecommercebackend.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long>, JpaSpecificationExecutor<Branch> {

    Optional<Branch> findByName(String name);

    Optional<Branch> findByOrderPrefix(String orderPrefix);

    List<Branch> findByIsDeletedFalse();

    /**
     * Busca una sucursal por su ID usando una consulta nativa para ignorar la
     * cláusula @Where (is_deleted = false). Esto garantiza que los desarrolladores
     * puedan ver los registros eliminados.
     * @param id El ID de la sucursal a buscar.
     * @return Un Optional con la sucursal, si existe.
     */
    @Query(value = "SELECT * FROM branches WHERE branch_id = :id", nativeQuery = true)
    Optional<Branch> findByIdIncludingDeleted(@Param("id") Long id);
}