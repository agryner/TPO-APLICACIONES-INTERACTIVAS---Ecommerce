package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class ResenaDuplicadaException extends ExcepcionDeNegocio {
    public ResenaDuplicadaException() {
        super(HttpStatus.CONFLICT, "Ya dejaste una resena de ese producto en esta orden");
    }
}
