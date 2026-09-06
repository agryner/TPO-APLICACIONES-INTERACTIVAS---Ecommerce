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

/**
 * Registro y login: los dos unicos lugares donde se emite un token.
 *
 * Va al lado de AutorizacionService porque son el par natural. Este responde
 * "quien sos" comprobandolo contra la base; el otro responde "que podes"
 * una vez que ya se sabe quien sos.
 *
 * No repite la logica de alta: se la pide a UsuarioService, que ya sabe
 * rechazar mails repetidos y forzar el rol CLIENTE. Aca solo se agrega el
 * token.
 */
@Service
@RequiredArgsConstructor
public class AutenticacionService {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Crea la cuenta y devuelve el token ya emitido.
     *
     * Se devuelve el token directamente para que registrarse e iniciar sesion
     * sean un solo paso: si no, el frontend tendria que encadenar dos pedidos
     * con los mismos datos.
     *
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
     * Verifica mail y contrasena, y devuelve un token si cierran.
     *
     * authenticate es quien compara: busca al usuario por mail con el
     * UserDetailsService y le pasa la contrasena al PasswordEncoder. Si algo no
     * da, tira una AuthenticationException y no se llega a emitir nada.
     *
     * Tambien rechaza a las cuentas dadas de baja, porque Usuario.isEnabled
     * devuelve el campo activo.
     *
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

    /**
     * Trae el usuario recien creado para poder firmarle el token.
     *
     * Pre : el id.
     * Post: la entidad. Tira UsuarioNoEncontradoException si no esta, que no
     *       deberia pasar porque se acaba de guardar.
     */
    private Usuario buscar(Long id) throws UsuarioNoEncontradoException {
        return usuarioRepository.findById(id).orElseThrow(UsuarioNoEncontradoException::new);
    }
}
