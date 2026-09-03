package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tiran los services cuando un ADMIN intenta publicar, carritear o comprar.
 *
 * No es que le falten permisos: le sobran, y ese es el problema. Un admin que
 * vende puede aprobarse sus propias fotos y despacharse sus propias ordenes, y
 * uno que compra audita transacciones en las que es parte. Separar los dos
 * papeles evita tener que confiar en que no los mezcle.
 *
 * Es 403 y no 409 porque depende de quien pide, no del estado de la cosa: el
 * mismo producto lo puede comprar cualquier CLIENTE sin problema.
 */
@ResponseStatus(code = HttpStatus.FORBIDDEN,
        reason = "Un administrador no puede comprar ni vender")
public class AdminNoComerciaException extends Exception {
}
