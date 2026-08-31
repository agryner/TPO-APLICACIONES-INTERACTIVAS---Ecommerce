package com.uade.tpo.marketplace.service;

import java.util.List;
import java.util.Optional;

import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.controllers.UsuarioRequest;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.UsuarioDuplicadoException;

/**
 * Contrato de la logica de usuarios.
 *
 * Lo consume UsuariosController y lo implementa UsuarioServiceImpl.
 */
public interface UsuarioService {

    List<Usuario> getUsuarios();

    Optional<Usuario> getUsuarioById(Long idUsuario);

    Usuario createUsuario(UsuarioRequest request) throws UsuarioDuplicadoException;

    Usuario updateUsuario(Long idUsuario, UsuarioRequest request) throws UsuarioNoEncontradoException;

    void deleteUsuario(Long idUsuario) throws UsuarioNoEncontradoException;
}
