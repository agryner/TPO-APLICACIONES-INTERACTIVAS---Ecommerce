package com.uade.tpo.marketplace.controllers.dashboard;

import java.math.BigDecimal;
import java.util.List;

import com.uade.tpo.marketplace.controllers.resenas.ResenaResponse;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardResponse {
    private RequiereAccion requiereAccion;
    private Plata plata;
    private Rendimiento rendimiento;
    private Reputacion reputacion;

    @Data
    @AllArgsConstructor
    public static class RequiereAccion {
        private long borradoresSinFoto;
        private long aDespachar;
        private long aCoordinar;
        private long fotosEnRevision;
    }

    @Data
    @AllArgsConstructor
    public static class Plata {
        private BigDecimal ingresos;
        private BigDecimal ingresosEsteMes;
        private long ventas;
        private long pendientesDeCobro;
        private long canceladas;
    }

    @Data
    @AllArgsConstructor
    public static class Rendimiento {
        private long visitas;
        private Double conversion;
        private long guardadosEnWishlist;
        private ProductoDestacado masVendido;
        private ProductoDestacado masVisto;
    }

    @Data
    @AllArgsConstructor
    public static class Reputacion {
        private Double promedio;
        private long cantidadResenas;
        private Double diasPromedioDespacho;
        private List<ResenaResponse> ultimasResenas;
    }

    @Data
    @AllArgsConstructor
    public static class ProductoDestacado {
        private Long id;
        private String nombre;
        private long cantidad;
    }
}
