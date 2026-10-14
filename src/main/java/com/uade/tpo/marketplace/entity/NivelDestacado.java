package com.uade.tpo.marketplace.entity;

import java.math.BigDecimal;

public enum NivelDestacado {
    NINGUNO(0, "0"),
    BASICO(1, "5000"),
    DESTACADO(2, "12000"),
    PREMIUM(3, "25000");

    private final int peso;
    // Cuanto cuesta un mes de este nivel, en pesos.
    private final BigDecimal precioMensual;

    NivelDestacado(int peso, String precioMensual) {
        this.peso = peso;
        this.precioMensual = new BigDecimal(precioMensual);
    }

    public int getPeso() {
        return peso;
    }

    public BigDecimal getPrecioMensual() {
        return precioMensual;
    }
}
