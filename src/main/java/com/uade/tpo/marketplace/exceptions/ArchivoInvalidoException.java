package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class ArchivoInvalidoException extends ExcepcionDeNegocio {
    public ArchivoInvalidoException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, mensaje);
    }
}
