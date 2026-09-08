package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tira OrdenDeCompraServiceImpl cuando se quiere cerrar una orden sin items.
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "No se puede generar una orden de un carrito sin items")
public class CarritoVacioException extends Exception {
}
