package com.uade.tpo.marketplace.entity.dto;

import lombok.Data;

/**
 * Datos que entran por el body al agregar un item al carrito.
 *
 * Trae el id del producto y la cantidad; CarritoServiceImpl los resuelve y
 * arma o actualiza el ItemCarrito correspondiente.
 */
@Data
public class ItemCarritoRequest {
    private Long idProducto;
    private Integer cantidad;
}
