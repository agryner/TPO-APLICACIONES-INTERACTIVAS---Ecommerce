package com.uade.tpo.marketplace.controllers.productos;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class ProductoRequest {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio tiene que ser mayor a cero")
    private BigDecimal precio;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;

    @Size(max = 500, message = "La descripcion no puede superar los 500 caracteres")
    private String descripcion;

    @Size(max = 150, message = "La ubicacion no puede superar los 150 caracteres")
    private String ubicacion;

    @Min(value = 0, message = "El descuento no puede ser negativo")
    @Max(value = 100, message = "El descuento no puede superar el 100 por ciento")
    private Integer descuento;

    @NotNull(message = "Hay que indicar la categoria")
    private Long idCategoria;
}
