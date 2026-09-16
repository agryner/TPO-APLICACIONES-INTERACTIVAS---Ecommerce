package com.uade.tpo.marketplace.controllers.carritos;

import com.uade.tpo.marketplace.entity.ItemCarrito;

import lombok.Data;
import com.uade.tpo.marketplace.controllers.productos.ProductoResumenResponse;

@Data
public class ItemCarritoResponse {
    private Long id;
    private Integer cantidad;
    private ProductoResumenResponse producto;

    /**
     * Pre : la entidad ItemCarrito, o null.
     * Post: el renglon con su cantidad y el producto en vista reducida. La
     *       cantidad es del renglon y no del producto: es cuantas unidades de
     *       ese producto metio este comprador en su carrito.
     */
    public static ItemCarritoResponse from(ItemCarrito item) {
        if (item == null)
            return null;

        ItemCarritoResponse dto = new ItemCarritoResponse();
        dto.setId(item.getId());
        dto.setCantidad(item.getCantidad());
        dto.setProducto(ProductoResumenResponse.from(item.getProducto()));
        return dto;
    }
}
