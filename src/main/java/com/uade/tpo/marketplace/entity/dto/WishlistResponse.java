package com.uade.tpo.marketplace.entity.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.uade.tpo.marketplace.entity.Wishlist;

import lombok.Data;

/**
 * Vista publica de la wishlist de un usuario.
 *
 * No trae subtotal ni total, a diferencia del carrito: guardar algo para mas
 * adelante no es una compra en curso, y sumar precios que van a cambiar antes
 * de que alguien compre seria inventar un numero.
 */
@Data
public class WishlistResponse {

    private Long id;
    private Long idUsuario;
    private LocalDateTime fechaLimite;
    private int cantidadDeItems;
    private List<ItemWishlistResponse> items;

    public static WishlistResponse from(Wishlist wishlist) {
        if (wishlist == null)
            return null;

        WishlistResponse dto = new WishlistResponse();
        dto.setId(wishlist.getId());
        dto.setIdUsuario(wishlist.getUsuario() == null ? null : wishlist.getUsuario().getId());
        dto.setFechaLimite(wishlist.getFechaLimite());

        List<ItemWishlistResponse> items = wishlist.getItems() == null ? List.of()
                : wishlist.getItems().stream()
                        // lo ultimo guardado primero, que es como se mira una wishlist
                        .sorted((a, b) -> b.getFechaAgregado().compareTo(a.getFechaAgregado()))
                        .map(ItemWishlistResponse::from)
                        .toList();

        dto.setItems(items);
        dto.setCantidadDeItems(items.size());
        return dto;
    }
}
