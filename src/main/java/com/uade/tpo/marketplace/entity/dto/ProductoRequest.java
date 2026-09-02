package com.uade.tpo.marketplace.entity.dto;

import java.math.BigDecimal;

import lombok.Data;

/**
 * Datos que entran por el body al crear o editar un producto.
 *
 * Trae idCategoria en vez del objeto completo; el service lo resuelve contra
 * su repository antes de armar la entidad Producto.
 *
 * No trae vendedor a proposito: se publica siempre a nombre de quien hace el
 * pedido, asi que sale del idSolicitante y no puede falsearse desde el body.
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
}
