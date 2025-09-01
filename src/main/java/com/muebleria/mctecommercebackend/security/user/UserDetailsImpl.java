package com.muebleria.mctecommercebackend.security.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.muebleria.mctecommercebackend.model.User;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

@Data
public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    @JsonIgnore
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private boolean isEnabled;
    private Long managedBranchId;

    private String firstName;
    private String lastName;

    // CAMBIO: El constructor ahora acepta Long para el id
    public UserDetailsImpl(Long id, String username, String password,
                           Collection<? extends GrantedAuthority> authorities,
                           boolean isEnabled, Long managedBranchId, String firstName, String lastName) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.isEnabled = isEnabled;
        this.managedBranchId = managedBranchId;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public static UserDetailsImpl build(User user) {
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_"+user.getRole());

        // CAMBIO AQUÍ: user.getManagedBranch().getId() en lugar de .getBranchId()
        Long branchId = (user.getManagedBranch() != null) ? user.getManagedBranch().getId() : null;

        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(authority),
                user.isActive(),
                branchId,
                user.getProfile().getFirstName(),
                user.getProfile().getLastName()
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
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // La cuenta está habilitada si el campo 'isActive' es true en la BD
        return this.isEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(id, user.id);
    }
}