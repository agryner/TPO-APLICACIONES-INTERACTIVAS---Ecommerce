package com.uade.tpo.marketplace.entity.dto;

import java.time.LocalDateTime;

import com.uade.tpo.marketplace.entity.EstadoPublicacion;
import com.uade.tpo.marketplace.entity.ItemWishlist;
import com.uade.tpo.marketplace.entity.Producto;

import lombok.Data;

/**
 * Un renglon de la wishlist visto desde afuera.
 *
 * Ademas del producto lleva disponible, que resume en un booleano si hoy se
 * puede comprar. La wishlist conserva los productos que salieron de
 * circulacion, asi que el frontend necesita saber cuales mostrar apagados sin
 * tener que interpretar por su cuenta la combinacion de activo y
 * estadoPublicacion.
 */
@Data
public class ItemWishlistResponse {

    private Long id;
    private LocalDateTime fechaAgregado;
    private boolean disponible;
    private ProductoResponse producto;

    public static ItemWishlistResponse from(ItemWishlist item) {
        if (item == null)
            return null;

        ItemWishlistResponse dto = new ItemWishlistResponse();
        dto.setId(item.getId());
        dto.setFechaAgregado(item.getFechaAgregado());
        dto.setProducto(ProductoResponse.from(item.getProducto()));
        dto.setDisponible(estaDisponible(item.getProducto()));
        return dto;
    }

    private static boolean estaDisponible(Producto producto) {
        return producto != null
                && Boolean.TRUE.equals(producto.getActivo())
                && producto.getEstadoPublicacion() == EstadoPublicacion.PUBLICADO
                && producto.getVendedor() != null
                && Boolean.TRUE.equals(producto.getVendedor().getActivo())
                && producto.getStock() != null && producto.getStock() > 0;
    }
}
