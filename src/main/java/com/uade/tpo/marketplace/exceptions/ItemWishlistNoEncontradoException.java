package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class ItemWishlistNoEncontradoException extends ExcepcionDeNegocio {
    public ItemWishlistNoEncontradoException() {
        super(HttpStatus.NOT_FOUND, "No existe ese item en la wishlist");
    }
}
