package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tira FotoServiceImpl cuando la verificacion automatica concluye que la
 * foto no tiene nada que ver con la categoria del producto.
 *
 * Lleva el mensaje que redacto el modelo para el vendedor, que explica que vio
 * en la imagen. Llega al cliente gracias a spring.web.error.include-message.
 */
@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
public class FotoRechazadaException extends Exception {

    public FotoRechazadaException(String mensaje) {
        super(mensaje);
    }
}
