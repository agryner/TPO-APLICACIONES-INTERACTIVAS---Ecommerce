package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.usuarios.UsuarioRequest;
import com.uade.tpo.marketplace.controllers.usuarios.UsuarioResponse;
import com.uade.tpo.marketplace.entity.TipoUsuario;
import java.util.List;

import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.CambioDeRolInvalidoException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.UsuarioDuplicadoException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;

public interface UsuarioService {
    List<UsuarioResponse> getUsuarios(Long idSolicitante)
            throws UsuarioNoEncontradoException, AccesoDenegadoException;

    UsuarioResponse getUsuarioById(Long idUsuario, Long idSolicitante)
            throws UsuarioNoEncontradoException, AccesoDenegadoException;

    UsuarioResponse createUsuario(UsuarioRequest request) throws UsuarioDuplicadoException;

    UsuarioResponse updateUsuario(Long idUsuario, UsuarioRequest request)
            throws UsuarioNoEncontradoException, CuentaInactivaException;

    UsuarioResponse reactivarUsuario(Long idUsuario, Long idSolicitante)
            throws UsuarioNoEncontradoException, AccesoDenegadoException, CuentaInactivaException;

    UsuarioResponse cambiarRol(Long idUsuario, TipoUsuario rol, Long idSolicitante)
            throws UsuarioNoEncontradoException, AccesoDenegadoException, CuentaInactivaException,
            CambioDeRolInvalidoException;

    void deleteUsuario(Long idUsuario, Long idSolicitante)
            throws UsuarioNoEncontradoException, OperacionAjenaException, CuentaInactivaException;
}
