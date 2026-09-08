package com.uade.tpo.marketplace.controllers.carritos;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class ItemCarritoRequest {
    @NotNull(message = "Hay que indicar el producto")
    private Long idProducto;

    private Integer cantidad;
}
