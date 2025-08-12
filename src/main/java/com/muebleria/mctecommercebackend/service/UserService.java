package com.muebleria.mctecommercebackend.service;

import com.muebleria.mctecommercebackend.dto.*;
import com.muebleria.mctecommercebackend.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserService {

    UserDTO createUser(UserDTO userDTO);

    Optional<UserDTO> findById(Long id);

    Optional<UserDTO> findByUsername(String username);

    Page<UserDTO> findAll(Pageable pageable, UserStatus status);

    void deleteById(Long id);

    UserDTO updateUser(Long id, UserUpdateDTO userUpdateDTO);

    void updateOwnPassword(UserPasswordUpdateDTO passwordUpdateDTO);

    UserDTO updateUserProfile(Long userId, UserProfileUpdateDTO profileUpdateDTO);

    UserDTO updateDriverDetails(Long userId, DriverDetailUpdateDTO driverDetailUpdateDTO);
}