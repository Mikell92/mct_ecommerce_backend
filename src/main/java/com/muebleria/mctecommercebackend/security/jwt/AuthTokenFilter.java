package com.muebleria.mctecommercebackend.security.jwt;

import com.muebleria.mctecommercebackend.model.User;
import com.muebleria.mctecommercebackend.repository.UserRepository;
import com.muebleria.mctecommercebackend.security.user.UserDetailsServiceImpl;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.ZoneId;
import java.util.Date;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    @Autowired
    private UserRepository userRepository; // Necesitamos el repositorio para buscar el usuario

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // --- INICIO DE LA LÓGICA DE VALIDACIÓN DE SESIÓN ---
                User user = userRepository.findByUsername(username).orElse(null);
                Claims claims = jwtUtils.getClaimsFromJwtToken(jwt); // Necesitamos un método para obtener los claims
                Date issuedAt = claims.getIssuedAt();

                // Si el token fue emitido ANTES del último cambio de contraseña, es inválido.
                if (user != null && user.getPasswordChangedAt() != null &&
                        issuedAt.before(Date.from(user.getPasswordChangedAt().atZone(ZoneId.systemDefault()).toInstant()))) {

                    logger.warn("Token inválido para el usuario '{}' debido a un cambio de contraseña.", username);
                    // Dejamos que la cadena continúe, pero sin autenticar al usuario.
                    // Esto resultará en un 401 Unauthorized más adelante.
                } else {
                    // Si la validación es exitosa, procedemos a autenticar.
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
                // --- FIN DE LA LÓGICA DE VALIDACIÓN DE SESIÓN ---
            }
        } catch (Exception e) {
            logger.error("No se pudo establecer la autenticación del usuario: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}