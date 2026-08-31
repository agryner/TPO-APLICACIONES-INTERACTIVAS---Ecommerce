package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tira CarritoServiceImpl y OrdenDeCompraServiceImpl cuando se pide mas
 * cantidad que el stock disponible.
 *
 * No la atrapa nadie: el @ResponseStatus de abajo hace que Spring la
 * traduzca sola al codigo HTTP correspondiente antes de responder.
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "No hay stock suficiente del producto solicitado")
public class StockInsuficienteException extends Exception {
}
