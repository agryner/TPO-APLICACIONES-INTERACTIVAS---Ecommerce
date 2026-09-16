package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class OrdenNoEncontradaException extends ExcepcionDeNegocio {
    public OrdenNoEncontradaException() {
        super(HttpStatus.NOT_FOUND, "No existe una orden de compra con ese id");
    }
}
