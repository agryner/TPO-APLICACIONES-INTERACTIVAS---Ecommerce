package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tira CarritoServiceImpl cuando el usuario todavia no tiene carrito.
 *
 * No la atrapa nadie: el @ResponseStatus de abajo hace que Spring la
 * traduzca sola al codigo HTTP correspondiente antes de responder.
 */
@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "No existe un carrito con ese id")
public class CarritoNoEncontradoException extends Exception {
}
