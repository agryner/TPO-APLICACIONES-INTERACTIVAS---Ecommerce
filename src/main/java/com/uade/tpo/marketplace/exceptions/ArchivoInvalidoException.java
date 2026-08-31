package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A diferencia del resto de las excepciones, esta lleva un mensaje propio porque
 * el motivo del rechazo cambia segun el caso (archivo vacio, tipo no permitido,
 * falta el producto). El mensaje llega al cliente gracias a
 * spring.web.error.include-message=always.
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class ArchivoInvalidoException extends Exception {

    public ArchivoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
