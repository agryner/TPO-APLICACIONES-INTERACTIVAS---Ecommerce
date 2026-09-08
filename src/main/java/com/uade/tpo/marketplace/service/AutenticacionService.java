package com.uade.tpo.marketplace.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.controllers.auth.LoginRequest;
import com.uade.tpo.marketplace.controllers.auth.TokenResponse;
import com.uade.tpo.marketplace.controllers.usuarios.UsuarioRequest;
import com.uade.tpo.marketplace.exceptions.UsuarioDuplicadoException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.repository.UsuarioRepository;
import com.uade.tpo.marketplace.controllers.config.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AutenticacionService {
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Pre : el request con los datos de la cuenta nueva.
     * Post: el token ya emitido. Tira UsuarioDuplicadoException si el mail o el
     *       nombre de usuario ya estan tomados.
     */
    public TokenResponse registrar(UsuarioRequest request)
            throws UsuarioDuplicadoException, UsuarioNoEncontradoException {
        Long id = usuarioService.createUsuario(request).getId();
        Usuario usuario = buscar(id);

        return TokenResponse.from(jwtService.generarToken(usuario));
    }

    /**
     * Pre : el request con mail y contrasena.
     * Post: el token y los datos basicos. Tira AuthenticationException -que
     *       sale como 401- si el mail no existe, la contrasena no coincide o
     *       la cuenta esta dada de baja.
     */
    public TokenResponse login(LoginRequest request) throws UsuarioNoEncontradoException {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getMail(), request.getContrasena()));

        Usuario usuario = usuarioRepository.findByMail(request.getMail())
                .orElseThrow(UsuarioNoEncontradoException::new);

        return TokenResponse.from(jwtService.generarToken(usuario));
    }

    private Usuario buscar(Long id) throws UsuarioNoEncontradoException {
        return usuarioRepository.findById(id).orElseThrow(UsuarioNoEncontradoException::new);
    }
}
