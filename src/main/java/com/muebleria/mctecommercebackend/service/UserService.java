package com.muebleria.mctecommercebackend.service;

import com.muebleria.mctecommercebackend.dto.UserDTO;
import com.muebleria.mctecommercebackend.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User saveUser(UserDTO userDTO); // Crea o guarda un nuevo usuario (maneja el hashing de contraseña)
    Optional<User> findById(Integer id); // Busca un usuario por su ID primario
    Optional<User> findByUsername(String username); // Busca un usuario por su nombre de usuario
    List<User> findAllUsers(); // Obtiene una lista de todos los usuarios
    User updateUser(Integer id, UserDTO userDTO); // Actualiza la información de un usuario existente
    void deleteUser(Integer id); // Marca un usuario como eliminado (borrado lógico)
}