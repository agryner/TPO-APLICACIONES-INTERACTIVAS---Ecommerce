package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.uade.tpo.marketplace.entity.Producto;

/**
 * La tiran CarritoServiceImpl y OrdenDeCompraServiceImpl cuando alguien intenta
 * comprar un producto que el mismo publico.
 *
 * Los chequeos de la orden no alcanzan para detectarlo: preguntan si el
 * solicitante es el comprador y si es el vendedor, y cuando es las dos cosas
 * las dos respuestas dan true. Por eso la regla va antes, sobre el producto.
 *
 * Es 409 y no 400 porque el pedido esta bien formado: lo que no se puede es
 * comprar esa cosa en particular siendo quien sos.
 *
 * Lleva el nombre del producto por el mismo motivo que StockInsuficiente: en un
 * carrito de varios items, un mensaje generico obliga a adivinar cual fallo. No
 * fija un reason en el @ResponseStatus para no pisar ese texto.
 */
@ResponseStatus(code = HttpStatus.CONFLICT)
public class CompraPropiaException extends Exception {

    public CompraPropiaException(Producto producto) {
        super("No se puede comprar \"%s\": el producto es tuyo"
                .formatted(producto.getNombre()));
    }
}
