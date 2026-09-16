package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class CantidadInvalidaException extends ExcepcionDeNegocio {
    public CantidadInvalidaException() {
        super(HttpStatus.BAD_REQUEST, "La cantidad tiene que ser al menos 1");
    }
}
