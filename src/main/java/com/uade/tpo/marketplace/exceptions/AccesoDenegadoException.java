package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tiran los services cuando el usuario que pide la operacion no tiene el
 * rol necesario para ejecutarla.
 */
@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "Solo un administrador puede realizar esta operacion")
public class AccesoDenegadoException extends Exception {
}
