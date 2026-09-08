package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tira los services cuando no existe un usuario con ese id.
 */
@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "No existe un usuario con ese id")
public class UsuarioNoEncontradoException extends Exception {
}
