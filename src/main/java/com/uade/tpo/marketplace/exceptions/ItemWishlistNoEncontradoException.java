package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La tira WishlistServiceImpl cuando se quiere sacar un item que no esta en esa
 * wishlist.
 *
 * Se busca por wishlist e item a la vez, asi que tampoco se encuentra el item
 * de la lista de otro: pedirlo con el id ajeno da 404, no 403, porque para
 * quien pregunta ese item no existe.
 */
@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "No existe ese item en la wishlist")
public class ItemWishlistNoEncontradoException extends Exception {
}
