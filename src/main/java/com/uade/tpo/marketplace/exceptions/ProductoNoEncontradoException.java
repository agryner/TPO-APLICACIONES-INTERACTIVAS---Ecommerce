package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class ProductoNoEncontradoException extends ExcepcionDeNegocio {
    public ProductoNoEncontradoException() {
        super(HttpStatus.NOT_FOUND, "No existe un producto con ese id");
    }
}
