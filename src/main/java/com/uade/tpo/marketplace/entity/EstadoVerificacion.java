package com.uade.tpo.marketplace.entity;

public enum EstadoVerificacion {
    APROBADA,

    EN_REVISION,

    // La IA dijo que no corresponde a la categoria. Se guarda (no se ve en el
    // catalogo) para poder volver a verificarla si el vendedor corrige la
    // categoria del producto.
    RECHAZADA
}
