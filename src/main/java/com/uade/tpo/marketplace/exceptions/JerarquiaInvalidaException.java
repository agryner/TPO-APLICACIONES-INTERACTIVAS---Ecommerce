package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class JerarquiaInvalidaException extends ExcepcionDeNegocio {
    public JerarquiaInvalidaException() {
        super(HttpStatus.BAD_REQUEST, "Una categoria no puede depender de si misma ni de una de sus "
                + "subcategorias");
    }
}
