package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class SinResultadosException extends ExcepcionDeNegocio {
    public SinResultadosException(String mensaje) {
        super(HttpStatus.NOT_FOUND, mensaje);
    }
}
