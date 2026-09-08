package com.uade.tpo.marketplace.controllers.wishlist;

import java.time.LocalDateTime;

import com.uade.tpo.marketplace.entity.EstadoPublicacion;
import com.uade.tpo.marketplace.entity.ItemWishlist;
import com.uade.tpo.marketplace.entity.Producto;

import lombok.Data;
import com.uade.tpo.marketplace.controllers.productos.ProductoResponse;

@Data
public class ItemWishlistResponse {
    private Long id;
    private LocalDateTime fechaAgregado;
    private boolean disponible;
    private ProductoResponse producto;

    /**
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

    private static boolean estaDisponible(Producto producto) {
        return producto != null
                && Boolean.TRUE.equals(producto.getActivo())
                && producto.getEstadoPublicacion() == EstadoPublicacion.PUBLICADO
                && producto.getVendedor() != null
                && Boolean.TRUE.equals(producto.getVendedor().getActivo())
                && producto.getStock() != null && producto.getStock() > 0;
    }
}
