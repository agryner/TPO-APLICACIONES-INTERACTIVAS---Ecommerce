package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class UsuarioDuplicadoException extends ExcepcionDeNegocio {
    public UsuarioDuplicadoException() {
        super(HttpStatus.BAD_REQUEST, "Ya existe un usuario con ese mail o nombre de usuario");
    }
}
