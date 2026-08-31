package com.uade.tpo.marketplace.controllers;

import java.math.BigDecimal;

import lombok.Data;

/**
 * Datos que entran por el body al crear o editar un producto.
 *
 * Trae idCategoria e idVendedor en vez de los objetos completos; el service
 * los resuelve contra sus repositories antes de armar la entidad Producto.
 */
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
}
