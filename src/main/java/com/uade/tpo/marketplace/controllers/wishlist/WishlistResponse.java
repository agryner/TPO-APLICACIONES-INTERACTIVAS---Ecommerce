package com.uade.tpo.marketplace.controllers.wishlist;

import java.time.LocalDateTime;
import java.util.List;

import com.uade.tpo.marketplace.entity.Wishlist;

import lombok.Data;

@Data
public class WishlistResponse {
    private Long id;
    private Long idUsuario;
    private LocalDateTime fechaLimite;
    private int cantidadDeItems;
    private List<ItemWishlistResponse> items;

    /**
     * Pre : la entidad Wishlist, o null.
     * Post: la lista con sus items ordenados por lo mas reciente y la
     *       cantidad. No trae totales: guardar algo para despues no es una
     *       compra en curso.
     */
    public static WishlistResponse from(Wishlist wishlist) {
        if (wishlist == null)
            return null;

        WishlistResponse dto = new WishlistResponse();
        dto.setId(wishlist.getId());
        dto.setIdUsuario(wishlist.getUsuario() == null ? null : wishlist.getUsuario().getId());
        dto.setFechaLimite(wishlist.getFechaLimite());

        List<ItemWishlistResponse> items = wishlist.getItems() == null ? List.of()
                : wishlist.getItems().stream()
                        .sorted((a, b) -> b.getFechaAgregado().compareTo(a.getFechaAgregado()))
                        .map(ItemWishlistResponse::from)
                        .toList();

        dto.setItems(items);
        dto.setCantidadDeItems(items.size());
        return dto;
    }
}
