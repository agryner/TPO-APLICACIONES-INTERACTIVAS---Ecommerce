package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tira UsuarioServiceImpl cuando el mail o el nombre de usuario ya estan
 * tomados.
 *
 * No la atrapa nadie: el @ResponseStatus de abajo hace que Spring la
 * traduzca sola al codigo HTTP correspondiente antes de responder.
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Ya existe un usuario con ese mail o nombre de usuario")
public class UsuarioDuplicadoException extends Exception {
}
