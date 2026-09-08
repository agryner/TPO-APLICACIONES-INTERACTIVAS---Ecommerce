package com.uade.tpo.marketplace.controllers.carritos;

import com.uade.tpo.marketplace.entity.ItemCarrito;

import lombok.Data;
import com.uade.tpo.marketplace.controllers.productos.ProductoResponse;

@Data
public class ItemCarritoResponse {
    private Long id;
    private Integer cantidad;
    private ProductoResponse producto;

    /**
     * Pre : la entidad ItemCarrito, o null.
     * Post: el renglon con su cantidad y el producto aplanado.
     */
    public static ItemCarritoResponse from(ItemCarrito item) {
        if (item == null)
            return null;

        ItemCarritoResponse dto = new ItemCarritoResponse();
        dto.setId(item.getId());
        dto.setCantidad(item.getCantidad());
        dto.setProducto(ProductoResponse.from(item.getProducto()));
        return dto;
    }
}
