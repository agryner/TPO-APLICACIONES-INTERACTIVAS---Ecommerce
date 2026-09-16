package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.auth.LoginRequest;
import com.uade.tpo.marketplace.controllers.auth.TokenResponse;
import com.uade.tpo.marketplace.controllers.usuarios.UsuarioRequest;
import com.uade.tpo.marketplace.exceptions.UsuarioDuplicadoException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;

public interface AutenticacionService {
    TokenResponse registrar(UsuarioRequest request)
            throws UsuarioDuplicadoException, UsuarioNoEncontradoException;

    TokenResponse login(LoginRequest request) throws UsuarioNoEncontradoException;
}
