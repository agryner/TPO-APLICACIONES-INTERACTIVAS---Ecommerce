package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class PrecioOfrecidoInvalidoException extends ExcepcionDeNegocio {
    public PrecioOfrecidoInvalidoException() {
        super(HttpStatus.BAD_REQUEST, "La oferta tiene que ser menor al precio actual del producto");
    }
}
