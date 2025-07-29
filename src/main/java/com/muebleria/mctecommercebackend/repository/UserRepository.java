package com.muebleria.mctecommercebackend.repository;

import com.muebleria.mctecommercebackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// [CAMBIO] -> Importar la anotación @EntityGraph de Spring Data JPA
import org.springframework.data.jpa.repository.EntityGraph;

@Repository // Indica que esta interfaz es un componente de repositorio de Spring
public interface UserRepository extends JpaRepository<User, Integer> {
    // JpaRepository<TipoDeEntidad, TipoDeClavePrimaria>

    // Método personalizado. Spring Data JPA generará la consulta SQL por el nombre del método.
    // Útil para buscar usuarios por su nombre de usuario (ej. para login o verificar unicidad).
    // [CAMBIO] -> Usar @EntityGraph para cargar la relación 'managedBranch' explícitamente.
    // Esto asegura que cuando se busca un usuario, su sucursal gestionada también se recupera
    // de la base de datos en la misma consulta, evitando LazyInitializationException.
    @EntityGraph(attributePaths = "managedBranch")
    Optional<User> findByUsername(String username);
}