package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tira CarritoServiceImpl cuando se quiere borrar un item que no esta en
 * ese carrito.
 *
 * No la atrapa nadie: el @ResponseStatus de abajo hace que Spring la
 * traduzca sola al codigo HTTP correspondiente antes de responder.
 */
@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "El carrito no contiene un item con ese id")
public class ItemCarritoNoEncontradoException extends Exception {
}
