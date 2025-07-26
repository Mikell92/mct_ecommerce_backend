package com.muebleria.mctecommercebackend.security.user;

import com.muebleria.mctecommercebackend.model.User;
import com.muebleria.mctecommercebackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Esta clase carga los detalles del usuario para Spring Security.
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Busca el usuario en la base de datos por su nombre de usuario.
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con nombre: " + username));

        // Retorna una instancia de UserDetails (en este caso, UserDetailsImpl)
        // que Spring Security utilizará para la autenticación y autorización.
        return UserDetailsImpl.build(user);
    }
}