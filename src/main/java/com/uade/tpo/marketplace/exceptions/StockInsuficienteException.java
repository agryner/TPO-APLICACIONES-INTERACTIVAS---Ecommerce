package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.uade.tpo.marketplace.entity.Producto;

/**
 * La tiran CarritoServiceImpl y OrdenDeCompraServiceImpl cuando se pide mas
 * cantidad que el stock disponible.
 *
 * Lleva el nombre del producto y los numeros concretos: con un carrito de
 * varios items, un mensaje generico obliga al comprador a adivinar cual de
 * todos fallo. El mensaje llega al cliente gracias a
 * spring.web.error.include-message=always, y por eso el @ResponseStatus de
 * abajo no fija un reason: uno fijo pisaria el texto.
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class StockInsuficienteException extends Exception {

    public StockInsuficienteException(Producto producto, int pedida) {
        super("No hay stock suficiente de \"%s\": se pidieron %d y quedan %d"
                .formatted(producto.getNombre(), pedida, producto.getStock()));
    }
}
