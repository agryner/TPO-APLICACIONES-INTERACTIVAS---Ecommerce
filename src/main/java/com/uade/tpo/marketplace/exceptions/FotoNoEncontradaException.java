package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class FotoNoEncontradaException extends ExcepcionDeNegocio {
    public FotoNoEncontradaException() {
        super(HttpStatus.NOT_FOUND, "No existe una foto con ese id");
    }
}
