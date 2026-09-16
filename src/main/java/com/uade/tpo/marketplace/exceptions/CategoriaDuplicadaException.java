package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class CategoriaDuplicadaException extends ExcepcionDeNegocio {
    public CategoriaDuplicadaException() {
        super(HttpStatus.BAD_REQUEST, "La categoria que se intenta agregar esta duplicada");
    }
}
