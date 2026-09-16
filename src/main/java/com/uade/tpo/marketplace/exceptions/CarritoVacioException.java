package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class CarritoVacioException extends ExcepcionDeNegocio {
    public CarritoVacioException() {
        super(HttpStatus.BAD_REQUEST, "No se puede generar una orden de un carrito sin items");
    }
}
