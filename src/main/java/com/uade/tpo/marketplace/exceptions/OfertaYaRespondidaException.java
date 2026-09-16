package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class OfertaYaRespondidaException extends ExcepcionDeNegocio {
    public OfertaYaRespondidaException() {
        super(HttpStatus.CONFLICT, "Esa oferta ya fue respondida");
    }
}
