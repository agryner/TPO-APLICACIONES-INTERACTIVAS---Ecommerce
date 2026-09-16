package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

import com.uade.tpo.marketplace.entity.Producto;

public class StockInsuficienteException extends ExcepcionDeNegocio {
    public StockInsuficienteException(Producto producto, int pedida) {
        super(HttpStatus.BAD_REQUEST,
                "No hay stock suficiente de \"%s\": se pidieron %d y quedan %d"
                        .formatted(producto.getNombre(), pedida, producto.getStock()));
    }
}
