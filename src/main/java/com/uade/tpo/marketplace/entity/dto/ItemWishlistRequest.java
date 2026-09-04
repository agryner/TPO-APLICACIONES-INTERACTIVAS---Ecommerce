package com.uade.tpo.marketplace.entity.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

/**
 * Datos que entran por el body al guardar un producto en la wishlist.
 *
 * Solo el id del producto: la wishlist no tiene cantidades.
 */
@Data
public class ItemWishlistRequest {

    @NotNull(message = "Hay que indicar el producto")
    private Long idProducto;
}
