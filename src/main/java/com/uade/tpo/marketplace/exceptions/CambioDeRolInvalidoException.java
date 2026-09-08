package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT,
        reason = "Un administrador no puede quitarse el rol a si mismo")
public class CambioDeRolInvalidoException extends Exception {
}
