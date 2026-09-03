package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.entity.dto.UsuarioRequest;
import com.uade.tpo.marketplace.entity.dto.UsuarioResponse;
import com.uade.tpo.marketplace.entity.TipoUsuario;
import java.util.List;

import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.CambioDeRolInvalidoException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.UsuarioDuplicadoException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;

/**
 * Contrato de la logica de usuarios.
 *
 * Lo consume UsuariosController y lo implementa UsuarioServiceImpl.
 */
public interface UsuarioService {

    List<UsuarioResponse> getUsuarios();

    UsuarioResponse getUsuarioById(Long idUsuario) throws UsuarioNoEncontradoException;

    UsuarioResponse createUsuario(UsuarioRequest request) throws UsuarioDuplicadoException;

    /** Cada uno edita solo su propia cuenta. */
    UsuarioResponse updateUsuario(Long idUsuario, UsuarioRequest request, Long idSolicitante)
            throws UsuarioNoEncontradoException, OperacionAjenaException, CuentaInactivaException;

    /** Cada uno da de baja solo su propia cuenta. */
    /** Vuelve a poner en circulacion una cuenta dada de baja. Solo ADMIN. */
    UsuarioResponse reactivarUsuario(Long idUsuario, Long idSolicitante)
            throws UsuarioNoEncontradoException, AccesoDenegadoException, CuentaInactivaException;

    /**
     * Promueve o degrada a un usuario. Solo ADMIN, y en un endpoint aparte
     * porque el rol no puede viajar en el body de un alta ni de una edicion.
     */
    UsuarioResponse cambiarRol(Long idUsuario, TipoUsuario rol, Long idSolicitante)
            throws UsuarioNoEncontradoException, AccesoDenegadoException, CuentaInactivaException,
            CambioDeRolInvalidoException;

    void deleteUsuario(Long idUsuario, Long idSolicitante)
            throws UsuarioNoEncontradoException, OperacionAjenaException, CuentaInactivaException;
}
