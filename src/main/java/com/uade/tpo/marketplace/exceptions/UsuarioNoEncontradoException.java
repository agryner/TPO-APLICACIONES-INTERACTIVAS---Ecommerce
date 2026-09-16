package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class UsuarioNoEncontradoException extends ExcepcionDeNegocio {
    public UsuarioNoEncontradoException() {
        super(HttpStatus.NOT_FOUND, "No existe un usuario con ese id");
    }
}
