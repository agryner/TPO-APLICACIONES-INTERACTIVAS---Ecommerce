package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tira FotoServiceImpl cuando no existe una foto con ese id.
 */
@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "No existe una foto con ese id")
public class FotoNoEncontradaException extends Exception {
}
