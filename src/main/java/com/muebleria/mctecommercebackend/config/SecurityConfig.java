package com.muebleria.mctecommercebackend.config;

import com.muebleria.mctecommercebackend.security.jwt.AuthEntryPointJwt;
import com.muebleria.mctecommercebackend.security.jwt.AuthTokenFilter;
import com.muebleria.mctecommercebackend.security.jwt.JwtAccessDeniedHandler;
import com.muebleria.mctecommercebackend.security.jwt.TimeAccessFilter;
import com.muebleria.mctecommercebackend.security.user.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // Para @PreAuthorize
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity; // Asegura que esta anotación esté presente
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy; // Para sesiones sin estado
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // Para añadir nuestro filtro JWT
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration // Indica que esta clase es una clase de configuración
@EnableWebSecurity // Habilita la configuración de seguridad web de Spring Security
@EnableMethodSecurity // Habilita la seguridad a nivel de método (ej. @PreAuthorize)
public class SecurityConfig {

    @Autowired
    UserDetailsServiceImpl userDetailsService; // Nuestro servicio personalizado para cargar usuarios

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler; // Manejador de errores de autenticación

    @Autowired
    private JwtAccessDeniedHandler accessDeniedHandler; // Inyecta el AccessDeniedHandler

    @Autowired
    private TimeAccessFilter timeAccessFilter; // 2. INYECTA EL NUEVO FILTRO

    @Autowired
    private AuthTokenFilter authTokenFilter;

    @Bean // Configura el proveedor de autenticación
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService); // Usa nuestro UserDetailsService
        authProvider.setPasswordEncoder(passwordEncoder()); // Usa nuestro PasswordEncoder
        return authProvider;
    }

    @Bean // Obtiene el AuthenticationManager de la configuración de autenticación
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean // Define el codificador de contraseñas
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean // Configura la cadena de filtros de seguridad HTTP
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Añadimos el CorsFilter al principio de la cadena de seguridad
                .addFilterBefore(corsFilter(), UsernamePasswordAuthenticationFilter.class)
                .csrf(AbstractHttpConfigurer::disable) // Deshabilita CSRF para APIs REST
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedHandler) // Manejo de errores 401
                        .accessDeniedHandler(accessDeniedHandler)     // ¡Añade esto para 403 Forbidden!
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // JWT es sin estado
                .authorizeHttpRequests(authorize -> authorize

                        // Permitir el endpoint de autenticación (login)
                        .requestMatchers("/api/auth/**").permitAll()

                        // Todas las demás solicitudes requieren autenticación
                        .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider()); // Añade nuestro proveedor de autenticación

        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        http.addFilterAfter(timeAccessFilter, AuthTokenFilter.class);


        return http.build();
    }

    @Bean // Configura CORS (Cross-Origin Resource Sharing)
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:4200"); // Frontend Angular
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}