package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class RolNoComerciaException extends ExcepcionDeNegocio {
    public RolNoComerciaException() {
        super(HttpStatus.FORBIDDEN, "Tu rol no puede comprar ni vender: el ADMIN modera y el "
                + "DESPACHANTE mueve envios");
    }
}
