package com.uade.tpo.marketplace.controllers.ofertas;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import com.uade.tpo.marketplace.entity.Provincia;

import jakarta.validation.constraints.Size;

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

    private Provincia provinciaEntrega;

    @Size(max = 100, message = "La localidad no puede superar los 100 caracteres")
    private String localidadEntrega;

    @Size(max = 150, message = "La calle no puede superar los 150 caracteres")
    private String direccionEntrega;
}
