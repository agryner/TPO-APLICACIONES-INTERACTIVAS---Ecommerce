package com.uade.tpo.marketplace.controllers.productos;

import java.math.BigDecimal;

import com.uade.tpo.marketplace.entity.CondicionProducto;
import com.uade.tpo.marketplace.entity.Provincia;

import lombok.Data;

@Data
public class FiltroProductos {
    private Long idCategoria;
    private String nombre;
    private BigDecimal precioMin;
    private BigDecimal precioMax;
    private Boolean enOferta;
    private Provincia provincia;
    private CondicionProducto condicion;
    private Integer anioDesde;
    private Integer anioHasta;
    private Boolean admiteEnvio;
    private String orden;
}
