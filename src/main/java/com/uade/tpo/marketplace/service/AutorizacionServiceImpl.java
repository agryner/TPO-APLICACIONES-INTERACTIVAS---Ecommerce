package com.uade.tpo.marketplace.service;

import org.springframework.stereotype.Service;

import com.uade.tpo.marketplace.entity.TipoUsuario;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.RolNoComerciaException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AutorizacionServiceImpl implements AutorizacionService {
    private final UsuarioRepository usuarioRepository;

    /**
     * Pre : el id de quien pide la operacion.
     * Post: nada si la cuenta existe y esta vigente. Tira
     *       UsuarioNoEncontradoException o CuentaInactivaException si no.
     */
    public void validarActivo(Long idUsuario)
            throws UsuarioNoEncontradoException, CuentaInactivaException {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);

        if (!Boolean.TRUE.equals(usuario.getActivo()))
            throw new CuentaInactivaException();
    }

    /**
     * Pre : el id de quien pide y el id del duenio del recurso.
     * Post: nada si es el duenio, o si es ADMIN. Tira OperacionAjenaException
     *       en cualquier otro caso, y CuentaInactivaException si quien pide
     *       esta dado de baja.
     */
    public void validarDuenio(Long idSolicitante, Long idDuenio)
            throws OperacionAjenaException, UsuarioNoEncontradoException, CuentaInactivaException {
        if (idSolicitante == null)
            throw new OperacionAjenaException();

        validarActivo(idSolicitante);

        if (idSolicitante.equals(idDuenio))
            return;

        if (!esAdmin(idSolicitante))
            throw new OperacionAjenaException();
    }

    public boolean esAdmin(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .map(usuario -> usuario.getRol() == TipoUsuario.ADMIN)
                .orElse(false);
    }

    /**
     * Pre : el id de quien pide la operacion.
     * Post: nada si es un CLIENTE. Tira RolNoComerciaException si es ADMIN:
     *       el rol modera el marketplace, no participa de el.
     */
    public void validarQuePuedaComerciar(Long idSolicitante)
            throws UsuarioNoEncontradoException, RolNoComerciaException {
        TipoUsuario rol = usuarioRepository.findById(idSolicitante)
                .map(Usuario::getRol)
                .orElseThrow(UsuarioNoEncontradoException::new);

        if (rol == TipoUsuario.ADMIN || rol == TipoUsuario.DESPACHANTE)
            throw new RolNoComerciaException();
    }

    /**
     * Pre : el id de quien pide la operacion.
     * Post: nada si es ADMIN. Tira AccesoDenegadoException si no lo es, o
     *       UsuarioNoEncontradoException si el id no existe.
     */
    public void validarAdmin(Long idUsuario)
            throws UsuarioNoEncontradoException, AccesoDenegadoException {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);

        if (usuario.getRol() != TipoUsuario.ADMIN)
            throw new AccesoDenegadoException();
    }
}
