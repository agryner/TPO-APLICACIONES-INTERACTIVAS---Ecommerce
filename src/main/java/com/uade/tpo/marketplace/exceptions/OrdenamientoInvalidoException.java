package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class OrdenamientoInvalidoException extends ExcepcionDeNegocio {
    public OrdenamientoInvalidoException() {
        super(HttpStatus.BAD_REQUEST, "El parametro orden solo acepta precio_asc, precio_desc, "
                + "vistos o vendidos");
    }
}
