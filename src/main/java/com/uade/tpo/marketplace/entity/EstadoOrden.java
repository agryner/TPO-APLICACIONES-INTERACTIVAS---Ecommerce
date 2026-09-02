package com.uade.tpo.marketplace.entity;

/**
 * Estados por los que pasa una orden.
 *
 * El flujo normal es PENDIENTE -> PAGADA -> ENVIADA -> RECIBIDA. Se puede
 * cancelar mientras no haya salido el envio. RECIBIDA y CANCELADA son finales:
 * de ahi no se sale.
 *
 * Se guarda como texto gracias al @Enumerated(STRING) de OrdenDeCompra, asi que
 * agregar estados no rompe los datos existentes.
 */
public enum EstadoOrden {
    PENDIENTE,
    PAGADA,
    ENVIADA,
    RECIBIDA,
    CANCELADA
}
