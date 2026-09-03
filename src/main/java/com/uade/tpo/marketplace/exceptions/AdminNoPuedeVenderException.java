package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Se lanza cuando un usuario con rol ADMIN intenta publicar un producto.
 * Los administradores moderan fotos y administran categorías, no venden.
 */
@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "Los administradores no pueden publicar productos a la venta")
public class AdminNoPuedeVenderException extends Exception {
}