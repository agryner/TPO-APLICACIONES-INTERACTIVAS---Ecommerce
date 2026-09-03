package com.uade.tpo.marketplace.entity.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

/**
 * Datos que entran por el body al agregar un item al carrito.
 *
 * Trae el id del producto y la cantidad; CarritoServiceImpl los resuelve y
 * arma o actualiza el ItemCarrito correspondiente.
 *
 * La cantidad no lleva un minimo declarado porque significa cosas distintas
 * segun el endpoint: al agregar tiene que ser al menos 1, pero al modificar un
 * cero o un negativo son la forma de sacar el item. Ese limite lo pone
 * CarritoServiceImpl en agregarItem, que es quien sabe cual de los dos es.
 */
@Data
public class ItemCarritoRequest {

    @NotNull(message = "Hay que indicar el producto")
    private Long idProducto;

    private Integer cantidad;
}
