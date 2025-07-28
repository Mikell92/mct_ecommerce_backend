package com.muebleria.mctecommercebackend.controller;

import com.muebleria.mctecommercebackend.dto.CategoryDTO;
import com.muebleria.mctecommercebackend.exception.ResourceNotFoundException;
import com.muebleria.mctecommercebackend.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // Importar Page
import org.springframework.data.domain.Pageable; // Importar Pageable
import org.springframework.data.web.PageableDefault; // Importar PageableDefault
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión de categorías.
 * <p>
 * Proporciona endpoints para operaciones CRUD de categorías, con validaciones,
 * manejo de excepciones y autorización basada en roles de Spring Security.
 * </p>
 */
@RestController
@RequestMapping("/api/categories") // Ruta base para el controlador de categorías
public class CategoryController {

    private final CategoryService categoryService;

    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Crea una nueva categoría.
     * Requiere el rol 'ADMIN'.
     *
     * @param categoryDTO El DTO de la categoría a crear.
     * @return ResponseEntity con el DTO de la categoría creada y estado 201 Created.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        CategoryDTO savedCategory = categoryService.saveCategory(categoryDTO);
        return new ResponseEntity<>(savedCategory, HttpStatus.CREATED);
    }

    /**
     * Obtiene una categoría por su ID.
     * Permite los roles 'ADMIN', 'GESTOR_INVENTARIO' y 'CAJERO'.
     *
     * @param id El ID de la categoría.
     * @return ResponseEntity con el DTO de la categoría y estado 200 OK, o 404 Not Found si no existe.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_INVENTARIO', 'CAJERO')")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Integer id) {
        return categoryService.findById(id)
                .map(categoryDTO -> new ResponseEntity<>(categoryDTO, HttpStatus.OK))
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));
    }

    /**
     * **Modificado:** Obtiene una página de todas las categorías activas.
     * Permite los roles 'ADMIN', 'GESTOR_INVENTARIO' y 'CAJERO'.
     *
     * @param pageable Objeto Pageable inyectado automáticamente por Spring.
     * Configurado con valores por defecto de tamaño 10 y ordenado por nombre.
     * @return ResponseEntity con una Page de DTOs de categorías y estado 200 OK.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_INVENTARIO', 'CAJERO')")
    public ResponseEntity<Page<CategoryDTO>> getAllCategories(@PageableDefault(size = 10, sort = "name") Pageable pageable) {
        Page<CategoryDTO> categoriesPage = categoryService.findAllActiveCategories(pageable);
        return new ResponseEntity<>(categoriesPage, HttpStatus.OK);
    }

    /**
     * Actualiza una categoría existente.
     * Requiere el rol 'ADMIN'.
     *
     * @param id El ID de la categoría a actualizar.
     * @param categoryDTO El DTO con los datos actualizados.
     * @return ResponseEntity con el DTO de la categoría actualizada y estado 200 OK.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryDTO> updateCategory(@PathVariable Integer id, @Valid @RequestBody CategoryDTO categoryDTO) {
        CategoryDTO updatedCategory = categoryService.updateCategory(id, categoryDTO);
        return new ResponseEntity<>(updatedCategory, HttpStatus.OK);
    }

    /**
     * Realiza el borrado lógico de una categoría.
     * Requiere el rol 'ADMIN'.
     *
     * @param id El ID de la categoría a eliminar lógicamente.
     * @return ResponseEntity con estado 204 No Content.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Integer id) {
        categoryService.deleteCategory(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // ¡¡Los métodos @ExceptionHandler han sido eliminados de aquí!!
    // Ahora serán manejados por GlobalExceptionHandler.java

}