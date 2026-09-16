package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class ItemCarritoNoEncontradoException extends ExcepcionDeNegocio {
    public ItemCarritoNoEncontradoException() {
        super(HttpStatus.NOT_FOUND, "El carrito no contiene un item con ese id");
    }
}
