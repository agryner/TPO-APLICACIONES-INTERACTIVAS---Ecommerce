package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tira CategoriaServiceImpl cuando ya hay una categoria hermana con ese
 * nombre.
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "La categoria que se intenta agregar esta duplicada")
public class CategoriaDuplicadaException extends Exception {
}
