package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class OperacionAjenaException extends ExcepcionDeNegocio {
    public OperacionAjenaException() {
        super(HttpStatus.FORBIDDEN, "No se puede operar sobre recursos de otro usuario");
    }
}
