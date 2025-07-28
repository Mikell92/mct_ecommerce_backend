package com.muebleria.mctecommercebackend.repository;

import com.muebleria.mctecommercebackend.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de Spring Data JPA para la entidad {@link Category}.
 * <p>
 * Proporciona métodos CRUD automáticos y consultas personalizadas para la gestión de categorías.
 * </p>
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    /**
     * Busca una categoría por su nombre. Se utiliza para la validación de unicidad.
     *
     * @param name El nombre de la categoría a buscar.
     * @return Un {@link Optional} que contiene la categoría si se encuentra, o un Optional vacío.
     */
    Optional<Category> findByName(String name);
}