package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tira CarritoServiceImpl cuando se quiere agregar un item con cantidad
 * cero o negativa.
 *
 * No se valida con una anotacion en ItemCarritoRequest porque el mismo DTO lo
 * usa modificarCantidad, donde un cero es la forma legitima de sacar el item.
 * El limite depende del endpoint, asi que lo pone el service.
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "La cantidad tiene que ser al menos 1")
public class CantidadInvalidaException extends Exception {
}
