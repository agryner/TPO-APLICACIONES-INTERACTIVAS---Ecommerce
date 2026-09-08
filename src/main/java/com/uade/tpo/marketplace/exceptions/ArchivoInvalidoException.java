package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class ArchivoInvalidoException extends Exception {
    public ArchivoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
