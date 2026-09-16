package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class ProductoNoAceptaOfertasException extends ExcepcionDeNegocio {
    public ProductoNoAceptaOfertasException() {
        super(HttpStatus.CONFLICT, "Ese producto no acepta ofertas: el vendedor no lo puso "
                + "a negociar");
    }
}
