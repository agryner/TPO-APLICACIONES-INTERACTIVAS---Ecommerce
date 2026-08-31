package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tira CategoriaServiceImpl cuando el nuevo padre dejaria a la categoria
 * colgando de si misma.
 *
 * No la atrapa nadie: el @ResponseStatus de abajo hace que Spring la
 * traduzca sola al codigo HTTP correspondiente antes de responder.
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Una categoria no puede depender de si misma ni de una de sus subcategorias")
public class JerarquiaInvalidaException extends Exception {
}
