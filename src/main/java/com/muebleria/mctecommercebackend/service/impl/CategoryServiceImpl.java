package com.muebleria.mctecommercebackend.service.impl;

import com.muebleria.mctecommercebackend.dto.CategoryDTO;
import com.muebleria.mctecommercebackend.exception.ResourceNotFoundException;
import com.muebleria.mctecommercebackend.model.Category;
import com.muebleria.mctecommercebackend.repository.CategoryRepository;
import com.muebleria.mctecommercebackend.security.user.UserDetailsImpl; // Importación crucial
import com.muebleria.mctecommercebackend.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // Importar Page
import org.springframework.data.domain.PageImpl; // Importar PageImpl
import org.springframework.data.domain.Pageable; // Importar Pageable
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación de la interfaz {@link CategoryService} para la gestión de categorías.
 * <p>
 * Contiene la lógica de negocio, incluyendo validaciones, gestión de auditoría
 * y borrado lógico para las operaciones CRUD de categorías.
 * </p>
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Autowired
    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Método auxiliar para obtener el ID del usuario actualmente autenticado.
     * Es crucial para la auditoría automática.
     *
     * @return Un {@link Optional} que contiene el ID del usuario si está autenticado,
     * o un Optional vacío si no hay un usuario autenticado o el principal no es {@link UserDetailsImpl}.
     */
    private Optional<Integer> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return Optional.of(userDetails.getId());
        }
        return Optional.empty();
    }

    /**
     * Guarda una nueva categoría en la base de datos.
     * Realiza validación de unicidad del nombre y puebla los campos de auditoría.
     *
     * @param categoryDTO El DTO con la información de la nueva categoría.
     * @return El DTO de la categoría guardada.
     * @throws RuntimeException si el nombre de la categoría ya existe.
     */
    @Override
    public CategoryDTO saveCategory(CategoryDTO categoryDTO) {
        if (categoryRepository.findByName(categoryDTO.getName()).isPresent()) {
            throw new RuntimeException("El nombre de la categoría '" + categoryDTO.getName() + "' ya existe.");
        }

        Category category = new Category();
        category.setName(categoryDTO.getName());
        category.setIsDeleted(false); // Por defecto, una nueva categoría no está eliminada

        // Asignar el ID del usuario que la crea (Auditoría)
        getCurrentUserId().ifPresent(category::setCreatedByUserId);

        Category savedCategory = categoryRepository.save(category);
        return CategoryDTO.fromEntity(savedCategory); // Método de conversión estático (ver nota abajo)
    }

    /**
     * Busca una categoría por su ID.
     *
     * @param id El ID de la categoría.
     * @return Un {@link Optional} que contiene el DTO de la categoría si existe y no está
     * eliminada lógicamente, o un Optional vacío en caso contrario.
     */
    @Override
    public Optional<CategoryDTO> findById(Integer id) {
        return categoryRepository.findById(id)
                .filter(category -> !category.getIsDeleted()) // Solo devuelve si no está borrada lógicamente
                .map(CategoryDTO::fromEntity); // Convierte la entidad a DTO
    }

    /**
     * **Modificado:** Obtiene una página de categorías activas (no eliminadas lógicamente).
     *
     * @param pageable Objeto Pageable con la información de paginación.
     * @return Una Page de DTOs de categorías activas.
     */
    @Override
    public Page<CategoryDTO> findAllActiveCategories(Pageable pageable) {
        // Obtenemos todas las categorías del repositorio y las filtramos por isDeleted=false
        List<Category> activeCategories = categoryRepository.findAll().stream()
                .filter(category -> !category.getIsDeleted())
                .collect(Collectors.toList());

        // Implementación de paginación manual sobre la lista filtrada
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), activeCategories.size());
        List<CategoryDTO> pageContent = activeCategories.subList(start, end)
                .stream()
                .map(CategoryDTO::fromEntity)
                .collect(Collectors.toList());

        // Devolvemos un PageImpl de CategoryDTO
        return new PageImpl<>(pageContent, pageable, activeCategories.size());
    }

    /**
     * Actualiza una categoría existente.
     *
     * @param id El ID de la categoría a actualizar.
     * @param categoryDTO El DTO con los datos actualizados.
     * @return El DTO de la categoría actualizada.
     * @throws ResourceNotFoundException si la categoría no se encuentra o ya está eliminada lógicamente.
     * @throws RuntimeException si el nuevo nombre de la categoría ya está en uso.
     */
    @Override
    public CategoryDTO updateCategory(Integer id, CategoryDTO categoryDTO) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));

        // No se debe permitir actualizar una categoría que ya ha sido borrada lógicamente
        if (existingCategory.getIsDeleted()) {
            throw new ResourceNotFoundException("Categoría no encontrada con ID: " + id);
        }

        // Valida que el nuevo nombre no exista si se está cambiando
        if (!existingCategory.getName().equalsIgnoreCase(categoryDTO.getName()) &&
                categoryRepository.findByName(categoryDTO.getName()).isPresent()) {
            throw new RuntimeException("El nuevo nombre de la categoría '" + categoryDTO.getName() + "' ya existe.");
        }

        existingCategory.setName(categoryDTO.getName());

        // Asignar el valor de lastUpdatedAt de forma manual (para asegurar que no sea nulo)
        existingCategory.setLastUpdatedAt(LocalDateTime.now());

        // Auditoría: Asigna el ID del usuario que la actualiza
        getCurrentUserId().ifPresent(existingCategory::setUpdatedByUserId);

        Category updatedCategory = categoryRepository.save(existingCategory);
        return CategoryDTO.fromEntity(updatedCategory);
    }

    /**
     * Realiza el borrado lógico de una categoría.
     * En lugar de borrar el registro, marca el campo 'is_deleted' como true
     * y registra la fecha y el usuario que realizó la acción.
     *
     * @param id El ID de la categoría a eliminar lógicamente.
     * @throws ResourceNotFoundException si la categoría no se encuentra.
     */
    @Override
    public void deleteCategory(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));

        // Marcar como eliminado y registrar auditoría
        category.setIsDeleted(true);
        category.setDeletedAt(LocalDateTime.now());
        getCurrentUserId().ifPresent(category::setDeletedByUserId);

        categoryRepository.save(category);
    }
}