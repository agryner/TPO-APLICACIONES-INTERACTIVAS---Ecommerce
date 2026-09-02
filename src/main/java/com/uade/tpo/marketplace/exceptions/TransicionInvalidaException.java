package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tiran los services cuando el estado al que se quiere pasar no se puede
 * alcanzar desde el estado actual.
 *
 * La usan las ordenes (saltearse pasos del flujo, o tocar una ya cerrada) y las
 * publicaciones (pausar algo que todavia es borrador, por ejemplo).
 *
 * No la atrapa nadie: el @ResponseStatus de abajo hace que Spring la
 * traduzca sola al codigo HTTP correspondiente antes de responder.
 */
@ResponseStatus(code = HttpStatus.CONFLICT, reason = "No se puede pasar a ese estado desde el estado actual")
public class TransicionInvalidaException extends Exception {
}
