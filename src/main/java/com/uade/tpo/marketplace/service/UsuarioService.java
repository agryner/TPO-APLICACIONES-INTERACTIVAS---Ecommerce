package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.entity.dto.UsuarioRequest;
import com.uade.tpo.marketplace.entity.dto.UsuarioResponse;
import java.util.List;

import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.UsuarioDuplicadoException;

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
            throws UsuarioNoEncontradoException, OperacionAjenaException;

    /** Cada uno da de baja solo su propia cuenta. */
    void deleteUsuario(Long idUsuario, Long idSolicitante)
            throws UsuarioNoEncontradoException, OperacionAjenaException;
}
