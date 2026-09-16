package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class CuentaInactivaException extends ExcepcionDeNegocio {
    public CuentaInactivaException() {
        super(HttpStatus.FORBIDDEN, "La cuenta esta dada de baja");
    }
}
