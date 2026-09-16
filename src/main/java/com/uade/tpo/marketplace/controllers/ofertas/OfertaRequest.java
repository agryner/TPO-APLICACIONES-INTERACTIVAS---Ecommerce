package com.uade.tpo.marketplace.controllers.ofertas;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OfertaRequest {
    @NotNull(message = "Hay que indicar el producto")
    private Long idProducto;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad tiene que ser al menos 1")
    private Integer cantidad;

    @NotNull(message = "El precio ofrecido es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio ofrecido tiene que ser mayor a cero")
    private BigDecimal precioOfrecido;
}
