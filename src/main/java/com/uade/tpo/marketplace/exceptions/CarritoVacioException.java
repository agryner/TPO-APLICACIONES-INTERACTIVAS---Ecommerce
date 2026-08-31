package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tira OrdenDeCompraServiceImpl cuando se quiere cerrar una orden sin items.
 *
 * No la atrapa nadie: el @ResponseStatus de abajo hace que Spring la
 * traduzca sola al codigo HTTP correspondiente antes de responder.
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "No se puede generar una orden de un carrito sin items")
public class CarritoVacioException extends Exception {
}
