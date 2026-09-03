package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tira UsuarioServiceImpl cuando un ADMIN intenta quitarse el rol a si
 * mismo.
 *
 * No es una cuestion de permisos: el admin tiene permiso de sobra. Es que si
 * el unico administrador se degrada, no queda nadie que pueda volver a
 * promover a nadie y el sistema se cierra por fuera de la base de datos.
 */
@ResponseStatus(code = HttpStatus.CONFLICT,
        reason = "Un administrador no puede quitarse el rol a si mismo")
public class CambioDeRolInvalidoException extends Exception {
}
