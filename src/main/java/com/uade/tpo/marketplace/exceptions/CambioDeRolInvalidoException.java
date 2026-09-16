package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class CambioDeRolInvalidoException extends ExcepcionDeNegocio {
    public CambioDeRolInvalidoException() {
        super(HttpStatus.CONFLICT, "Un administrador no puede quitarse el rol a si mismo");
    }
}
