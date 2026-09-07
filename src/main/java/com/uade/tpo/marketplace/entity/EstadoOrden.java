package com.uade.tpo.marketplace.entity;

/**
 * Estados por los que pasa una orden.
 *
 * La orden representa la PLATA, no la logistica: por eso solo tiene los estados
 * del cobro. Que la mercaderia se despacho, esta en transito o llego son hechos
 * del envio, que se modela aparte y con su propio responsable.
 *
 * El flujo es PENDIENTE -> PAGADA, y se puede cancelar desde cualquiera de los
 * dos mientras no haya despacho. PAGADA y CANCELADA son finales:
 * de ahi no se sale.
 *
 * Se guarda como texto gracias al @Enumerated(STRING) de OrdenDeCompra, asi que
 * agregar estados no rompe los datos existentes.
 */
public enum EstadoOrden {
    PENDIENTE,
    PAGADA,
    CANCELADA
}
