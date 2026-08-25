package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Una categoria no puede depender de si misma ni de una de sus subcategorias")
public class JerarquiaInvalidaException extends Exception {
}
