package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FORBIDDEN,
        reason = "Un administrador no puede comprar ni vender")
public class AdminNoComerciaException extends Exception {
}
