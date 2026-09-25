package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class EnvioNoDisponibleException extends ExcepcionDeNegocio {
    public EnvioNoDisponibleException() {
        super(HttpStatus.CONFLICT, "Ese envio no esta esperando para retirar: o ya lo tomo "
                + "otro, o el vendedor todavia no lo despacho");
    }
}
