package com.uade.tpo.marketplace.controllers.wishlist;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class ItemWishlistRequest {
    @NotNull(message = "Hay que indicar el producto")
    private Long idProducto;
}
