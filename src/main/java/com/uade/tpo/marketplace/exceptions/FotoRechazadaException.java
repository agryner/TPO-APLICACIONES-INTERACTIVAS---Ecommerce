package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
public class FotoRechazadaException extends Exception {
    public FotoRechazadaException(String mensaje) {
        super(mensaje);
    }
}
