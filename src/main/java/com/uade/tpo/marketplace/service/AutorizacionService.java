package com.uade.tpo.marketplace.service;

import org.springframework.stereotype.Service;

import com.uade.tpo.marketplace.entity.TipoUsuario;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
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
     * Corta la operacion si la cuenta que la pide esta dada de baja.
     *
     * Va antes que cualquier otro chequeo: quien no deberia estar operando no
     * tiene por que llegar a que se le evalue la pertenencia.
     */
    public void validarActivo(Long idUsuario)
            throws UsuarioNoEncontradoException, CuentaInactivaException {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);

        if (!Boolean.TRUE.equals(usuario.getActivo()))
            throw new CuentaInactivaException();
    }

    /**
     * Corta la operacion si quien la pide no es el duenio del recurso.
     *
     * Es el chequeo que impide editar el producto de otro vendedor, entrar al
     * carrito ajeno o dar de baja la cuenta de otro. De paso corta si la cuenta
     * que pide esta dada de baja.
     */
    public void validarDuenio(Long idSolicitante, Long idDuenio)
            throws OperacionAjenaException, UsuarioNoEncontradoException, CuentaInactivaException {
        if (idSolicitante == null || !idSolicitante.equals(idDuenio))
            throw new OperacionAjenaException();

        // Ser el duenio no alcanza: la cuenta tambien tiene que estar vigente.
        // Va aca y no en cada service porque este metodo es el cuello por el
        // que ya pasan todas las operaciones sobre algo propio; repartir el
        // chequeo por fuera garantizaba olvidarse de alguna, que fue justo lo
        // que paso.
        validarActivo(idSolicitante);
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

    /**
     * Deja pasar al duenio del recurso o a un ADMIN.
     *
     * Es el permiso de las operaciones que normalmente son personales pero que
     * la moderacion tambien necesita, como dar de baja una cuenta. Se separa de
     * validarDuenio porque la mayoria de las operaciones NO deben abrirse al
     * admin: el rol no lo vuelve duenio de los productos ni de los carritos de
     * los demas.
     */
    public void validarDuenioOAdmin(Long idSolicitante, Long idDuenio)
            throws OperacionAjenaException, UsuarioNoEncontradoException, CuentaInactivaException {
        if (idSolicitante != null && esAdmin(idSolicitante)) {
            validarActivo(idSolicitante);
            return;
        }

        validarDuenio(idSolicitante, idDuenio);
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
