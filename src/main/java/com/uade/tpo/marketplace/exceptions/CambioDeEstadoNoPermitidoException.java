package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tira OrdenDeCompraServiceImpl cuando quien pide el cambio de estado no
 * tiene por que hacerlo: o no es parte de la orden, o le corresponde a la otra
 * parte. El envio lo declara el vendedor y la recepcion el comprador.
 *
 * No la atrapa nadie: el @ResponseStatus de abajo hace que Spring la
 * traduzca sola al codigo HTTP correspondiente antes de responder.
 */
@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "Ese cambio de estado no le corresponde a este usuario")
public class CambioDeEstadoNoPermitidoException extends Exception {
}
