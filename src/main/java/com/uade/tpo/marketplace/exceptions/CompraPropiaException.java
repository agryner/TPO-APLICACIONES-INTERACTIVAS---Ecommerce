package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.uade.tpo.marketplace.entity.Producto;

@ResponseStatus(code = HttpStatus.CONFLICT)
public class CompraPropiaException extends Exception {
    public CompraPropiaException(Producto producto) {
        super("No se puede comprar \"%s\": el producto es tuyo"
                .formatted(producto.getNombre()));
    }
}
