package com.uade.tpo.marketplace.entity.dto;

import com.uade.tpo.marketplace.entity.ItemCarrito;

import lombok.Data;

/**
 * Renglon del carrito visto desde afuera.
 *
 * No incluye el carrito que lo contiene: seria una referencia circular, porque
 * CarritoResponse ya trae la lista de items.
 */
@Data
public class ItemCarritoResponse {

    private Long id;
    private Integer cantidad;
    private ProductoResponse producto;

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
