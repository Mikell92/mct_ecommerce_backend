package com.muebleria.mctecommercebackend.service.impl;

import com.muebleria.mctecommercebackend.dto.DriverDTO;
import com.muebleria.mctecommercebackend.exception.ResourceNotFoundException;
import com.muebleria.mctecommercebackend.model.Driver;
import com.muebleria.mctecommercebackend.repository.DriverRepository;
import com.muebleria.mctecommercebackend.security.user.UserDetailsImpl;
import com.muebleria.mctecommercebackend.service.DriverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación de la interfaz {@link DriverService} para la gestión de chóferes.
 * <p>
 * Contiene la lógica de negocio, incluyendo validaciones de unicidad, gestión de auditoría
 * y borrado lógico para las operaciones CRUD de chóferes.
 * </p>
 */
@Service
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;

    @Autowired
    public DriverServiceImpl(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
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
     * Guarda un nuevo chófer en la base de datos.
     * Realiza validación de unicidad del nombre y la licencia, y puebla los campos de auditoría.
     *
     * @param driverDTO El DTO con la información del nuevo chófer.
     * @return El DTO del chófer guardado.
     * @throws RuntimeException si el nombre o la licencia del chófer ya existen.
     */
    @Override
    public DriverDTO saveDriver(DriverDTO driverDTO) {
        // Validación de unicidad del nombre (opcional, pero buena práctica si el nombre es semánticamente único)
        if (driverRepository.findByNameAndIsDeletedFalse(driverDTO.getName()).isPresent()) {
            throw new RuntimeException("Ya existe un chófer con el nombre: " + driverDTO.getName());
        }
        // Validación de unicidad de la licencia si se proporciona
        if (driverDTO.getLicense() != null && driverRepository.existsByLicenseAndIsDeletedFalse(driverDTO.getLicense())) {
            throw new RuntimeException("Ya existe un chófer con la licencia: " + driverDTO.getLicense());
        }

        Driver driver = new Driver();
        driver.setName(driverDTO.getName());
        driver.setPhone(driverDTO.getPhone());
        driver.setLicense(driverDTO.getLicense());
        driver.setIsActive(driverDTO.getIsActive() != null ? driverDTO.getIsActive() : true); // Asegura valor por defecto si no se envía
        driver.setIsDeleted(false);

        // Asignar el ID del usuario que lo crea (Auditoría)
        getCurrentUserId().ifPresent(driver::setCreatedByUserId);

        Driver savedDriver = driverRepository.save(driver);
        return DriverDTO.fromEntity(savedDriver);
    }

    /**
     * Busca un chófer por su ID.
     *
     * @param id El ID del chófer.
     * @return Un {@link Optional} que contiene el DTO del chófer si existe y no está
     * eliminado lógicamente, o un Optional vacío en caso contrario.
     */
    @Override
    public Optional<DriverDTO> findById(Integer id) {
        return driverRepository.findById(id)
                .filter(driver -> !driver.getIsDeleted())
                .map(DriverDTO::fromEntity);
    }

    /**
     * Busca un chófer por su número de licencia.
     *
     * @param license El número de licencia del chófer.
     * @return Un {@link Optional} que contiene el DTO del chófer si existe y no está
     * eliminado lógicamente, o un Optional vacío en caso contrario.
     */
    @Override
    public Optional<DriverDTO> findByLicense(String license) {
        return driverRepository.findByLicenseAndIsDeletedFalse(license)
                .map(DriverDTO::fromEntity);
    }

    /**
     * Obtiene una página de chóferes activos (no eliminados lógicamente).
     *
     * @param pageable Objeto Pageable con la información de paginación.
     * @return Una Page de DTOs de chóferes activos.
     */
    @Override
    public Page<DriverDTO> findAllActiveDrivers(Pageable pageable) {
        // En este caso, podemos usar directamente el método del repositorio que ya filtra por isDeletedFalse
        // lo cual es más eficiente que traer todo a memoria y luego filtrar.
        return driverRepository.findByIsDeletedFalse(pageable)
                .map(DriverDTO::fromEntity); // Convierte cada entidad a DTO
    }

    /**
     * Actualiza un chófer existente.
     *
     * @param id El ID del chófer a actualizar.
     * @param driverDTO El DTO con los datos actualizados.
     * @return El DTO del chófer actualizado.
     * @throws ResourceNotFoundException si el chófer no se encuentra o ya está eliminado lógicamente.
     * @throws RuntimeException si el nuevo número de licencia ya está en uso por otro chófer.
     */
    @Override
    public DriverDTO updateDriver(Integer id, DriverDTO driverDTO) {
        Driver existingDriver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chófer no encontrado con ID: " + id));

        // No se debe permitir actualizar un chófer que ya ha sido borrado lógicamente
        if (existingDriver.getIsDeleted()) {
            throw new ResourceNotFoundException("Chófer no encontrado con ID: " + id);
        }

        // Valida que el nuevo número de licencia no exista si se está cambiando
        if (driverDTO.getLicense() != null && !driverDTO.getLicense().equalsIgnoreCase(existingDriver.getLicense())) {
            if (driverRepository.existsByLicenseAndDriverIdNotAndIsDeletedFalse(driverDTO.getLicense(), id)) {
                throw new RuntimeException("Ya existe un chófer con la licencia: " + driverDTO.getLicense());
            }
        }

        existingDriver.setName(driverDTO.getName());
        existingDriver.setPhone(driverDTO.getPhone());
        existingDriver.setLicense(driverDTO.getLicense());
        existingDriver.setIsActive(driverDTO.getIsActive()); // Actualiza el estado activo

        // Auditoría: Asigna el ID del usuario que lo actualiza
        getCurrentUserId().ifPresent(existingDriver::setUpdatedByUserId);

        Driver updatedDriver = driverRepository.save(existingDriver);
        return DriverDTO.fromEntity(updatedDriver);
    }

    /**
     * Realiza el borrado lógico de un chófer.
     * En lugar de borrar el registro, marca el campo 'is_deleted' como true
     * y registra la fecha y el usuario que realizó la acción.
     *
     * @param id El ID del chófer a eliminar lógicamente.
     * @throws ResourceNotFoundException si el chófer no se encuentra.
     */
    @Override
    public void deleteDriver(Integer id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chófer no encontrado con ID: " + id));

        driver.setIsDeleted(true);
        driver.setDeletedAt(LocalDateTime.now());
        getCurrentUserId().ifPresent(driver::setDeletedByUserId);

        driverRepository.save(driver);
    }
}