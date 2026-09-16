package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class CategoriaConProductosException extends ExcepcionDeNegocio {
    public CategoriaConProductosException() {
        super(HttpStatus.CONFLICT, "No se puede eliminar una categoria que tiene productos");
    }
}
