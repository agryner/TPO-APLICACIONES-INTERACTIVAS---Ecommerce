package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tira AutorizacionService cuando quien pide la operacion tiene la cuenta
 * dada de baja.
 *
 * Hasta ahora nadie miraba el campo activo al recibir el idSolicitante, asi que
 * un usuario dado de baja seguia comprando, publicando y operando con total
 * normalidad. La baja solo lo sacaba de los listados.
 */
@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "La cuenta esta dada de baja")
public class CuentaInactivaException extends Exception {
}
