package com.uade.tpo.marketplace.entity;

public enum NivelDestacado {
    NINGUNO(0),
    BASICO(1),
    DESTACADO(2),
    PREMIUM(3);

    private final int peso;

    NivelDestacado(int peso) {
        this.peso = peso;
    }

    public int getPeso() {
        return peso;
    }
}
