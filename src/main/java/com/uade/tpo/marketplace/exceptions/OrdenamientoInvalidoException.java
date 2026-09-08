package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "El parametro ordenPrecio solo acepta 'asc' o 'desc'")
public class OrdenamientoInvalidoException extends Exception {
}
