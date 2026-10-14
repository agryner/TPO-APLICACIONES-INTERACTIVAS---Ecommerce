package com.uade.tpo.marketplace.entity;

public enum TipoNotificacion {
    BAJA_DE_PRECIO,
    POCO_STOCK,
    DISPONIBLE_OTRA_VEZ,
    OFERTA_RECIBIDA,
    OFERTA_RESPONDIDA,
    // Al vendedor, cuando le compran.
    VENTA,
    // A las dos partes, cuando la orden se paga o se cancela.
    ORDEN_ACTUALIZADA,
    // A las dos partes, en cada paso del envio.
    ENVIO_ACTUALIZADO,
    // Al comprador, cuando le llega lo que compro.
    OPINA_TU_COMPRA,
    // Al vendedor, cuando la IA termina de revisar una foto.
    FOTO_VERIFICADA,
    // A los admins, cuando una foto queda esperando revision manual.
    FOTO_PENDIENTE
}
