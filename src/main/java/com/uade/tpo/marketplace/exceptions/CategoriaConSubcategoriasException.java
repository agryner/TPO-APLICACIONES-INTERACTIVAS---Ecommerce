package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tira CategoriaServiceImpl cuando se quiere borrar una categoria que
 * todavia tiene hijas.
 */
@ResponseStatus(code = HttpStatus.CONFLICT, reason = "No se puede eliminar una categoria que tiene subcategorias")
public class CategoriaConSubcategoriasException extends Exception {
}
