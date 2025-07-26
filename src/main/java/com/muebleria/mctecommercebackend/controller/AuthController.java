package com.muebleria.mctecommercebackend.controller;

import com.muebleria.mctecommercebackend.dto.JwtResponse;
import com.muebleria.mctecommercebackend.dto.LoginRequest;
import com.muebleria.mctecommercebackend.security.jwt.JwtUtils;
import com.muebleria.mctecommercebackend.security.user.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth") // Ruta base para el controlador de autenticación
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager; // Gestiona el proceso de autenticación

    @Autowired
    JwtUtils jwtUtils; // Utilidad para generar y validar JWT

    @PostMapping("/login") // Endpoint para iniciar sesión
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        // 1. Autenticar al usuario con las credenciales proporcionadas
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        // 2. Establecer la autenticación en el contexto de seguridad de Spring
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Generar el token JWT
        String jwt = jwtUtils.generateJwtToken(authentication);

        // 4. Obtener los detalles del usuario autenticado
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // 5. Extraer los roles/autoridades del usuario
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // 6. Retornar el JWT y la información del usuario en la respuesta
        return ResponseEntity.ok(new JwtResponse(jwt, "Bearer",
                userDetails.getId(),
                userDetails.getUsername(),
                roles.isEmpty() ? null : roles.get(0))); // Asumiendo un solo rol principal por simplicidad en JwtResponse
    }
}