package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class CambioDeRolInvalidoException extends ExcepcionDeNegocio {
    public CambioDeRolInvalidoException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje);
    }
}
