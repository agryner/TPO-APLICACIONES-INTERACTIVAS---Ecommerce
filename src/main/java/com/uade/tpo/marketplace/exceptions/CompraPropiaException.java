package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

import com.uade.tpo.marketplace.entity.Producto;

public class CompraPropiaException extends ExcepcionDeNegocio {
    public CompraPropiaException(Producto producto) {
        super(HttpStatus.CONFLICT, "No se puede comprar \"%s\": el producto es tuyo"
                .formatted(producto.getNombre()));
    }
}
