package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tira CategoriaServiceImpl cuando se quiere borrar una categoria que
 * todavia tiene productos publicados.
 *
 * Sin este control la baja llega a la base y la rompe la foreign key de
 * producto.id_categoria, que sale como un 500 con el SQL adentro.
 *
 * No la atrapa nadie: el @ResponseStatus de abajo hace que Spring la
 * traduzca sola al codigo HTTP correspondiente antes de responder.
 */
@ResponseStatus(code = HttpStatus.CONFLICT, reason = "No se puede eliminar una categoria que tiene productos")
public class CategoriaConProductosException extends Exception {
}
