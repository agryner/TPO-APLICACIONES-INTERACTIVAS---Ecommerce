package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class AccesoDenegadoException extends ExcepcionDeNegocio {
    public AccesoDenegadoException() {
        super(HttpStatus.FORBIDDEN, "Solo un administrador puede realizar esta operacion");
    }
}
