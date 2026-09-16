package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class CambioDeEstadoNoPermitidoException extends ExcepcionDeNegocio {
    public CambioDeEstadoNoPermitidoException() {
        super(HttpStatus.FORBIDDEN, "Ese cambio de estado no le corresponde a este usuario");
    }
}
