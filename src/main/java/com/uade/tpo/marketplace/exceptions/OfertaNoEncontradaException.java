package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class OfertaNoEncontradaException extends ExcepcionDeNegocio {
    public OfertaNoEncontradaException() {
        super(HttpStatus.NOT_FOUND, "No existe una oferta con ese id");
    }
}
