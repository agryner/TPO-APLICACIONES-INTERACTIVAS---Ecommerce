package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tira CarritoServiceImpl cuando se quiere borrar un item que no esta en
 * ese carrito.
 */
@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "El carrito no contiene un item con ese id")
public class ItemCarritoNoEncontradoException extends Exception {
}
