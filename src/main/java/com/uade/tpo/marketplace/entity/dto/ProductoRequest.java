package com.uade.tpo.marketplace.entity.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class ProductoRequest {
    private String nombre;
    private BigDecimal precio;
    private Integer stock;
    private String descripcion;
    private String ubicacion;
    private Integer descuento;
    private Long idCategoria;
    private Long idVendedor;
    private List<String> fotos;
}
