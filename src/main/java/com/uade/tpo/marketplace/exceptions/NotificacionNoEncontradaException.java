package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class NotificacionNoEncontradaException extends ExcepcionDeNegocio {
    public NotificacionNoEncontradaException() {
        super(HttpStatus.NOT_FOUND, "No existe una notificacion con ese id");
    }
}
