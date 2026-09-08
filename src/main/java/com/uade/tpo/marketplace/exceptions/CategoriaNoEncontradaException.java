package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tira CategoriaServiceImpl cuando no existe una categoria con ese id.
 */
@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "No existe una categoria con ese id")
public class CategoriaNoEncontradaException extends Exception {
}
