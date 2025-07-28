package com.muebleria.mctecommercebackend.service;

import com.muebleria.mctecommercebackend.dto.CategoryDTO;
import com.muebleria.mctecommercebackend.model.Category;
import org.springframework.data.domain.Page; // Importar la interfaz Page
import org.springframework.data.domain.Pageable; // Importar la interfaz Pageable

import java.util.List;
import java.util.Optional;

/**
 * Interfaz de servicio para la gestión de categorías.
 * <p>
 * Define las operaciones de negocio para la creación, lectura, actualización y eliminación
 * lógica de categorías, así como la obtención de listas y la validación de unicidad.
 * </p>
 */
public interface CategoryService {

    /**
     * Guarda una nueva categoría.
     *
     * @param categoryDTO El DTO de la categoría a guardar.
     * @return El DTO de la categoría guardada.
     */
    CategoryDTO saveCategory(CategoryDTO categoryDTO);

    /**
     * Busca una categoría por su ID. Solo devuelve categorías no eliminadas lógicamente.
     *
     * @param id El ID de la categoría.
     * @return Un {@link Optional} que contiene el DTO de la categoría si se encuentra y no está eliminada,
     * o un Optional vacío.
     */
    Optional<CategoryDTO> findById(Integer id);

    // **Modificado:** Ahora devuelve una Page de CategoryDTO en lugar de List<CategoryDTO>
    Page<CategoryDTO> findAllActiveCategories(Pageable pageable);

    /**
     * Actualiza una categoría existente.
     *
     * @param id El ID de la categoría a actualizar.
     * @param categoryDTO El DTO con los datos actualizados.
     * @return El DTO de la categoría actualizada.
     */
    CategoryDTO updateCategory(Integer id, CategoryDTO categoryDTO);

    /**
     * Realiza el borrado lógico de una categoría.
     *
     * @param id El ID de la categoría a eliminar lógicamente.
     */
    void deleteCategory(Integer id);
}