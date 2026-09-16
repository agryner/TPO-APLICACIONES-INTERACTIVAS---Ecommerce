package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class EntregaPendienteException extends ExcepcionDeNegocio {
    public EntregaPendienteException() {
        super(HttpStatus.CONFLICT, "Todavia no se puede calificar: el envio no figura como entregado");
    }
}
