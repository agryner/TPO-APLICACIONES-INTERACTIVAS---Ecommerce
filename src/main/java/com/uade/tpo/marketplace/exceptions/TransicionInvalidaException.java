package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "No se puede pasar a ese estado desde el estado actual")
public class TransicionInvalidaException extends Exception {
}
