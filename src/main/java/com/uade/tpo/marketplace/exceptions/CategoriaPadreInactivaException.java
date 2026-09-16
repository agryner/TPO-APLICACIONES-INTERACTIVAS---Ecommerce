package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class CategoriaPadreInactivaException extends ExcepcionDeNegocio {
    public CategoriaPadreInactivaException() {
        super(HttpStatus.CONFLICT, "No se puede reactivar una categoria cuyo padre esta dado de "
                + "baja: reactiva primero el padre");
    }
}
