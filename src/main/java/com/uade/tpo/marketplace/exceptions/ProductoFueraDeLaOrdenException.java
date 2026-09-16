package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class ProductoFueraDeLaOrdenException extends ExcepcionDeNegocio {
    public ProductoFueraDeLaOrdenException() {
        super(HttpStatus.BAD_REQUEST, "Ese producto no forma parte de esa orden");
    }
}
