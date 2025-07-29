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

    // [CAMBIO] -> Nueva propiedad para almacenar el ID de la sucursal gestionada
    private Integer managedBranchId;

    // [CAMBIO] -> Constructor actualizado para incluir managedBranchId
    public UserDetailsImpl(Integer id, String username, String password,
                           Collection<? extends GrantedAuthority> authorities,
                           Boolean isDeleted, Integer managedBranchId) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.isDeleted = isDeleted;
        this.managedBranchId = managedBranchId; // Inicializar la nueva propiedad
    }

    // [CAMBIO] -> Método build() actualizado para obtener y pasar managedBranchId
    public static UserDetailsImpl build(User user) {
        // Convertimos el rol del usuario (String) a una colección de GrantedAuthority
        GrantedAuthority authority = new SimpleGrantedAuthority(user.getRole()); // Se puede agregar/quitar el prefijo "ROLE_"

        // Obtener el ID de la sucursal gestionada si existe
        Integer branchId = null;
        if (user.getManagedBranch() != null) {
            branchId = user.getManagedBranch().getBranchId();
        }

        return new UserDetailsImpl(
                user.getUserId(),
                user.getUsername(),
                user.getPasswordHash(),
                Collections.singletonList(authority),
                user.getIsDeleted(),
                branchId // Pasar el ID de la sucursal gestionada
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