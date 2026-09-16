package com.uade.tpo.marketplace.controllers.resenas;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResenaRequest {
    @NotNull(message = "Hay que indicar la orden")
    private Long idOrden;

    @NotNull(message = "Hay que indicar el producto")
    private Long idProducto;

    @NotNull(message = "El puntaje es obligatorio")
    @Min(value = 1, message = "El puntaje minimo es 1")
    @Max(value = 5, message = "El puntaje maximo es 5")
    private Integer puntaje;

    @Size(max = 500, message = "El comentario no puede superar los 500 caracteres")
    private String comentario;
}
