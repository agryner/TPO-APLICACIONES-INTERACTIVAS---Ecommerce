package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class CategoriaNoEncontradaException extends ExcepcionDeNegocio {
    public CategoriaNoEncontradaException() {
        super(HttpStatus.NOT_FOUND, "No existe una categoria con ese id");
    }
}
