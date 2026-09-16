package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class EnvioNoEncontradoException extends ExcepcionDeNegocio {
    public EnvioNoEncontradoException() {
        super(HttpStatus.NOT_FOUND, "No existe un envio con ese id");
    }
}
