package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class ChatNoDisponibleException extends ExcepcionDeNegocio {
    public ChatNoDisponibleException(String motivo) {
        super(HttpStatus.CONFLICT, motivo);
    }
}
