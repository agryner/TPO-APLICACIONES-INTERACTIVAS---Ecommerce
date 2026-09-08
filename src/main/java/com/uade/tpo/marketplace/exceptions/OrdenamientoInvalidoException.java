package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tira ProductoServiceImpl cuando el parametro de ordenamiento no es ni
 * "asc" ni "desc".
 *
 * Se avisa en vez de ignorarlo en silencio: un error de tipeo devolveria el
 * listado sin ordenar y el cliente no tendria forma de darse cuenta.
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "El parametro ordenPrecio solo acepta 'asc' o 'desc'")
public class OrdenamientoInvalidoException extends Exception {
}
