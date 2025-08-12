package com.muebleria.mctecommercebackend.controller;

import com.muebleria.mctecommercebackend.dto.*;
import com.muebleria.mctecommercebackend.exception.ResourceNotFoundException;
import com.muebleria.mctecommercebackend.model.UserStatus;
import com.muebleria.mctecommercebackend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserDTO userDTO) {
        return new ResponseEntity<>(userService.createUser(userDTO), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO userDTO = userService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        return ResponseEntity.ok(userDTO);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<Page<UserDTO>> getAllUsers(
            @PageableDefault(size = 10, sort = "username") Pageable pageable,
            @RequestParam(defaultValue = "ACTIVE") UserStatus status) {

        Page<UserDTO> usersPage = userService.findAll(pageable, status);
        return ResponseEntity.ok(usersPage);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        return ResponseEntity.ok(userService.updateUser(id, userUpdateDTO));
    }

    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateOwnPassword(@Valid @RequestBody UserPasswordUpdateDTO passwordUpdateDTO) {
        userService.updateOwnPassword(passwordUpdateDTO);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/profile")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<UserDTO> updateUserProfile(@PathVariable Long id, @Valid @RequestBody UserProfileUpdateDTO profileUpdateDTO) {
        return ResponseEntity.ok(userService.updateUserProfile(id, profileUpdateDTO));
    }

    @PutMapping("/{id}/driver-details")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<UserDTO> updateDriverDetails(@PathVariable Long id, @Valid @RequestBody DriverDetailUpdateDTO driverDetailUpdateDTO) {
        return ResponseEntity.ok(userService.updateDriverDetails(id, driverDetailUpdateDTO));
    }
}