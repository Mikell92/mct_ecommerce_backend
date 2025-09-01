package com.muebleria.mctecommercebackend.controller;

import com.muebleria.mctecommercebackend.dto.JwtResponse;
import com.muebleria.mctecommercebackend.dto.LoginRequest;
import com.muebleria.mctecommercebackend.security.jwt.JwtUtils;
import com.muebleria.mctecommercebackend.security.user.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        // Manejo de Excepción de Horario
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new JwtResponse(jwt, "Bearer",
                    userDetails.getId(),
                    userDetails.getUsername(),
                    roles.isEmpty() ? null : roles.get(0),
                    userDetails.getFirstName(),
                    userDetails.getLastName()
            ));

        } catch (LockedException e) {
            // Atrapamos específicamente el error de cuenta bloqueada (por horario)
            // y devolvemos un mensaje claro.
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Acceso Denegado");
            errorResponse.put("message", e.getMessage()); // Usamos el mensaje de la excepción.
            return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
        }
    }
}