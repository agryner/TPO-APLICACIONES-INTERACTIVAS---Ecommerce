package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tiran los services cuando alguien intenta operar sobre algo que no es
 * suyo: el producto de otro vendedor, el carrito de otro comprador, la cuenta
 * de otra persona.
 *
 * Se separa de AccesoDenegadoException, que es para cuando falta el rol ADMIN:
 * aca el rol no tiene nada que ver, lo que falla es la pertenencia.
 */
@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "No se puede operar sobre recursos de otro usuario")
public class OperacionAjenaException extends Exception {
}
