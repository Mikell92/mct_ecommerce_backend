package com.muebleria.mctecommercebackend.service;

import com.muebleria.mctecommercebackend.dto.*;
import com.muebleria.mctecommercebackend.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserService {

    UserDTO createUser(UserDTO userDTO);

    boolean isUsernameTaken(String username);

    Optional<UserProfileViewDTO> findMyProfileById(Long id);

    Optional<UserDTO> findById(Long id);

    Optional<UserDTO> findByUsername(String username);

    Page<UserSummaryDTO> findAll(Pageable pageable, UserStatus status, String search, String role, Long branchId);

    void deleteById(Long id);

    UserDTO updateUser(Long id, UserUpdateDTO userUpdateDTO);

    void updateOwnPassword(UserPasswordUpdateDTO passwordUpdateDTO);

    void updateUserPassword(Long userId, AdminPasswordUpdateDTO passwordUpdateDTO);

    UserDTO updateOwnProfile(UserProfileUpdateDTO profileUpdateDTO);

    UserDTO updateUserProfile(Long userId, UserProfileUpdateDTO profileUpdateDTO);

    UserDTO updateDriverDetails(Long userId, DriverDetailUpdateDTO driverDetailUpdateDTO);

    UserDTO restoreUserById(Long id);

}