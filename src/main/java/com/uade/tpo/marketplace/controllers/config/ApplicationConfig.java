package com.uade.tpo.marketplace.controllers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.uade.tpo.marketplace.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Las piezas que Spring Security necesita para autenticar.
 *
 * Son cuatro y encajan asi: el UserDetailsService sabe traer un usuario por su
 * mail, el PasswordEncoder sabe comparar una contrasena contra su hash, el
 * AuthenticationProvider combina los dos, y el AuthenticationManager es lo que
 * el login termina llamando.
 */
@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UsuarioRepository usuarioRepository;

    /**
     * Como se busca un usuario a partir de lo que dice el token.
     *
     * Devuelve la entidad Usuario directamente, porque implementa UserDetails.
     *
     * Pre : nada; devuelve una funcion que Spring llama con el mail cuando
     *       necesita resolver una identidad.
     * Post: el bean que busca el Usuario por mail. Devuelve la entidad tal
     *       cual, porque implementa UserDetails. Tira
     *       UsernameNotFoundException si no hay ninguno con ese mail.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return mail -> usuarioRepository.findByMail(mail)
                .orElseThrow(() -> new UsernameNotFoundException("No existe un usuario con ese mail"));
    }

    /**
     * BCrypt: incluye una sal distinta en cada hash, asi que dos usuarios con la
     * misma contrasena tienen hashes distintos, y es lento a proposito para que
     * probar contrasenas por fuerza bruta no rinda.
     *
     * Nunca se desencripta: para verificar, se hashea lo que llega y se compara.
     *
     * Pre : nada.
     * Post: el bean de BCrypt, que se usa tanto para hashear al dar de alta
     *       como para comparar al loguear.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Pre : nada; toma los dos beans de arriba.
     * Post: el bean que combina de donde salen los usuarios con como estan
     *       guardadas las contrasenas. Es lo que efectivamente autentica.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Pre : la AuthenticationConfiguration que arma Spring.
     * Post: el bean que AutenticacionService llama en el login.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
