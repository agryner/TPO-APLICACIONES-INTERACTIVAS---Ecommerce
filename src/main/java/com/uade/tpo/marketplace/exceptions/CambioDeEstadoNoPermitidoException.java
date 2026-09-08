package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "Ese cambio de estado no le corresponde a este usuario")
public class CambioDeEstadoNoPermitidoException extends Exception {
}
