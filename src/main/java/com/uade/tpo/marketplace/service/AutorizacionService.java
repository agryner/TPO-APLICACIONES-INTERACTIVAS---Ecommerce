package com.uade.tpo.marketplace.service;

import org.springframework.stereotype.Service;

import com.uade.tpo.marketplace.entity.TipoUsuario;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Chequeos de permisos que necesitan varios services.
 *
 * Mientras no haya autenticacion, el id de quien pide la operacion llega como
 * parametro desde el controller. Cuando se sume el token, el idUsuario va a
 * salir de ahi y esta clase no cambia.
 */
@Service
@RequiredArgsConstructor
public class AutorizacionService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Corta la operacion si quien la pide no es el duenio del recurso.
     *
     * Es el chequeo que impide editar el producto de otro vendedor, entrar al
     * carrito ajeno o dar de baja la cuenta de otro.
     */
    public void validarDuenio(Long idSolicitante, Long idDuenio) throws OperacionAjenaException {
        if (idSolicitante == null || !idSolicitante.equals(idDuenio))
            throw new OperacionAjenaException();
    }

    /**
     * Responde si el usuario es ADMIN, sin cortar nada.
     *
     * Es la contracara de validarAdmin: esa se usa cuando el rol habilita una
     * operacion prohibida para el resto, esta cuando el rol no prohibe nada
     * sino que amplia lo que se ve, como el listado de ordenes. Un id que no
     * existe no es admin, asi que devuelve false en vez de explotar.
     */
    public boolean esAdmin(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .map(usuario -> usuario.getRol() == TipoUsuario.ADMIN)
                .orElse(false);
    }

    /** Corta la operacion si el usuario que la pide no es ADMIN. */
    public void validarAdmin(Long idUsuario)
            throws UsuarioNoEncontradoException, AccesoDenegadoException {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);

        if (usuario.getRol() != TipoUsuario.ADMIN)
            throw new AccesoDenegadoException();
    }
}
