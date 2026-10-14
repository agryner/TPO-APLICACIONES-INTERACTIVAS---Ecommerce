package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class ChatNoDisponibleException extends ExcepcionDeNegocio {
    public ChatNoDisponibleException() {
        super(HttpStatus.CONFLICT, "El chat se habilita cuando la orden esta pagada");
    }
}
