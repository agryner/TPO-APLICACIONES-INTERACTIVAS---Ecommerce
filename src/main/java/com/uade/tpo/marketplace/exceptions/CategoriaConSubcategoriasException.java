package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class CategoriaConSubcategoriasException extends ExcepcionDeNegocio {
    public CategoriaConSubcategoriasException() {
        super(HttpStatus.CONFLICT, "No se puede eliminar una categoria que tiene subcategorias");
    }
}
