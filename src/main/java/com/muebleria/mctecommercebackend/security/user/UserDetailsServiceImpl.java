package com.muebleria.mctecommercebackend.security.user;

import com.muebleria.mctecommercebackend.model.User;
import com.muebleria.mctecommercebackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// [CAMBIO] -> Importar la anotación @Transactional
import org.springframework.transaction.annotation.Transactional;

// Esta clase carga los detalles del usuario para Spring Security.
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    // [CAMBIO] -> Marcar el método como @Transactional
    // Esto asegura que la sesión de JPA esté abierta durante la ejecución de este método.
    // Aunque ya usamos @EntityGraph en UserRepository para cargar managedBranch,
    // es una buena práctica para métodos que interactúan con la base de datos
    // y para asegurar que otras relaciones perezosas puedan cargarse si fuera necesario.
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Busca el usuario en la base de datos por su nombre de usuario.
        // Gracias a @EntityGraph en UserRepository, la relación managedBranch
        // ya debería cargarse aquí.
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con nombre: " + username));

        // Retorna una instancia de UserDetails (en este caso, UserDetailsImpl)
        // que Spring Security utilizará para la autenticación y autorización.
        // UserDetailsImpl.build() ahora utiliza el 'managedBranch' cargado.
        return UserDetailsImpl.build(user);
    }
}