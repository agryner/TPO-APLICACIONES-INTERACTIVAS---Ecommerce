package com.uade.tpo.marketplace.controllers.wishlist;

import java.time.LocalDateTime;

import com.uade.tpo.marketplace.entity.EstadoPublicacion;
import com.uade.tpo.marketplace.entity.ItemWishlist;
import com.uade.tpo.marketplace.entity.Producto;

import lombok.Data;
import com.uade.tpo.marketplace.controllers.productos.ProductoResponse;

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

    /**
     * Traduce la entidad al objeto que sale por HTTP.
     *
     * Pre : la entidad ItemWishlist, o null.
     * Post: el renglon con el producto, cuando se guardo, y si hoy se puede
     *       comprar.
     */
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

    /**
     * Resume en un booleano si el producto se puede comprar ahora.
     *
     * Pre : el producto del item.
     * Post: true si esta activo, PUBLICADO, con stock y de un vendedor
     *       vigente. La wishlist conserva los que dejaron de estar
     *       disponibles, asi que el frontend necesita este dato para
     *       mostrarlos apagados en vez de esconderlos.
     */
    private static boolean estaDisponible(Producto producto) {
        return producto != null
                && Boolean.TRUE.equals(producto.getActivo())
                && producto.getEstadoPublicacion() == EstadoPublicacion.PUBLICADO
                && producto.getVendedor() != null
                && Boolean.TRUE.equals(producto.getVendedor().getActivo())
                && producto.getStock() != null && producto.getStock() > 0;
    }
}
