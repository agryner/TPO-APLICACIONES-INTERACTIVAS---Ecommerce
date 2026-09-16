package com.uade.tpo.marketplace.controllers.wishlist;

import java.time.LocalDateTime;

import com.uade.tpo.marketplace.entity.EstadoPublicacion;
import com.uade.tpo.marketplace.entity.ItemWishlist;
import com.uade.tpo.marketplace.entity.Producto;

import lombok.Data;
import com.uade.tpo.marketplace.controllers.productos.ProductoResumenResponse;

@Data
public class ItemWishlistResponse {
    private Long id;
    private LocalDateTime fechaAgregado;
    private ProductoResumenResponse producto;

    /**
     * Pre : la entidad ItemWishlist, o null.
     * Post: el renglon con el producto en vista reducida y cuando se
     *       guardo. Si hoy se puede comprar lo dice producto.disponible: la
     *       wishlist conserva los que dejaron de estarlo, asi que el front
     *       necesita ese dato para mostrarlos apagados en vez de esconderlos.
     */
    public static ItemWishlistResponse from(ItemWishlist item) {
        if (item == null)
            return null;

        ItemWishlistResponse dto = new ItemWishlistResponse();
        dto.setId(item.getId());
        dto.setFechaAgregado(item.getFechaAgregado());
        dto.setProducto(ProductoResumenResponse.from(item.getProducto()));
        return dto;
    }

}
