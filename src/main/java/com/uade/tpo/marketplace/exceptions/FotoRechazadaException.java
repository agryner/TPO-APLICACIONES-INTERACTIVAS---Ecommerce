package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class FotoRechazadaException extends ExcepcionDeNegocio {
    public FotoRechazadaException(String mensaje) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, mensaje);
    }
}
