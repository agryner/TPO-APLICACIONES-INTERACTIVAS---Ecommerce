package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class AnioInvalidoException extends ExcepcionDeNegocio {
    public AnioInvalidoException(int maximo) {
        super(HttpStatus.BAD_REQUEST, "El anio tiene que estar entre 1900 y " + maximo);
    }
}
