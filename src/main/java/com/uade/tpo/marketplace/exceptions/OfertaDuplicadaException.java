package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class OfertaDuplicadaException extends ExcepcionDeNegocio {
    public OfertaDuplicadaException() {
        super(HttpStatus.CONFLICT, "Ya tenes una oferta pendiente por ese producto");
    }
}
