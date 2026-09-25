package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class AccesoDenegadoException extends ExcepcionDeNegocio {
    public AccesoDenegadoException() {
        this("Solo un administrador puede realizar esta operacion");
    }

    public AccesoDenegadoException(String mensaje) {
        super(HttpStatus.FORBIDDEN, mensaje);
    }
}
