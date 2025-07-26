package com.muebleria.mctecommercebackend.security.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.muebleria.mctecommercebackend.model.User;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections; // Usaremos Collections.singletonList para un solo rol
import java.util.Objects;

@Data // Lombok para getters y setters
public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String username;

    @JsonIgnore // Evita que el passwordHash se serialice en JSON
    private String password;

    private Collection<? extends GrantedAuthority> authorities;
    private Boolean isDeleted; // Estado de eliminación del usuario

    // Constructor para crear una instancia de UserDetailsImpl
    public UserDetailsImpl(Integer id, String username, String password,
                           Collection<? extends GrantedAuthority> authorities, Boolean isDeleted) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.isDeleted = isDeleted;
    }

    // Método estático para construir UserDetailsImpl a partir de nuestra entidad User
    public static UserDetailsImpl build(User user) {
        // Convertimos el rol del usuario (String) a una colección de GrantedAuthority
        // Aquí, asumimos un solo rol por usuario.
        GrantedAuthority authority = new SimpleGrantedAuthority(user.getRole());
        return new UserDetailsImpl(
                user.getUserId(), // El userId de la entidad User
                user.getUsername(),
                user.getPasswordHash(), // Usamos el passwordHash como contraseña para Spring Security
                Collections.singletonList(authority), // Convierte un solo rol en una lista
                user.getIsDeleted() // Pasa el estado de isDeleted de la entidad User
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Podrías implementar lógica para cuentas expiradas aquí
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Podrías implementar lógica para cuentas bloqueadas aquí
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Podrías implementar lógica para credenciales expiradas aquí
    }

    @Override
    public boolean isEnabled() {
        // La cuenta está habilitada si NO está marcada como eliminada
        return !this.isDeleted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(id, user.id);
    }
}