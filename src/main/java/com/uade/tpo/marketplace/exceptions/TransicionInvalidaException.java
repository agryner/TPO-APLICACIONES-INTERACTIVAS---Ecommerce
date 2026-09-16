package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class TransicionInvalidaException extends ExcepcionDeNegocio {
    public TransicionInvalidaException() {
        super(HttpStatus.CONFLICT, "No se puede pasar a ese estado desde el estado actual");
    }
}
