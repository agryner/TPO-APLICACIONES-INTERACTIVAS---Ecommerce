package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.uade.tpo.marketplace.entity.Producto;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class StockInsuficienteException extends Exception {
    public StockInsuficienteException(Producto producto, int pedida) {
        super("No hay stock suficiente de \"%s\": se pidieron %d y quedan %d"
                .formatted(producto.getNombre(), pedida, producto.getStock()));
    }
}
