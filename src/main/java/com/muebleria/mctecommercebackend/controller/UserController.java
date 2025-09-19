package com.muebleria.mctecommercebackend.controller;

import com.muebleria.mctecommercebackend.dto.*;
import com.muebleria.mctecommercebackend.exception.ResourceNotFoundException;
import com.muebleria.mctecommercebackend.model.UserStatus;
import com.muebleria.mctecommercebackend.security.user.UserDetailsImpl;
import com.muebleria.mctecommercebackend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<Page<UserSummaryDTO>> getAllUsers(
            @PageableDefault(size = 10, sort = "username") Pageable pageable,
            @RequestParam(defaultValue = "ACTIVE") UserStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long branchId
    ) {

        Pageable finalPageable = pageable;

        if (status == UserStatus.DELETED) {
            finalPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("username").ascending());
        }

        // Pasamos los nuevos parámetros al servicio
        Page<UserSummaryDTO> usersPage = userService.findAll(finalPageable, status, search, role, branchId);
        return ResponseEntity.ok(usersPage);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserDTO userDTO) {
        return new ResponseEntity<>(userService.createUser(userDTO), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO userDTO = userService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        return ResponseEntity.ok(userDTO);
    }

    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        boolean isTaken = userService.isUsernameTaken(username);
        return ResponseEntity.ok(Map.of("isTaken", isTaken));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileViewDTO> getMyProfile(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        UserProfileViewDTO userDTO = userService.findMyProfileById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userDetails.getId()));
        return ResponseEntity.ok(userDTO);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<UserDTO> restoreUser(@PathVariable Long id) {
        UserDTO restoredUser = userService.restoreUserById(id);
        return ResponseEntity.ok(restoredUser);
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

    @PutMapping("/{id}/password")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<Void> updateUserPassword(@PathVariable Long id, @Valid @RequestBody AdminPasswordUpdateDTO passwordUpdateDTO) {
        userService.updateUserPassword(id, passwordUpdateDTO);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> updateMyProfile(@Valid @RequestBody UserProfileUpdateDTO profileUpdateDTO) {
        UserDTO updatedUser = userService.updateOwnProfile(profileUpdateDTO);
        return ResponseEntity.ok(updatedUser);
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