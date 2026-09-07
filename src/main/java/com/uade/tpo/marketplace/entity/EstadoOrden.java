package com.uade.tpo.marketplace.entity;

/**
 * Estados por los que pasa una orden.
 *
 * La orden representa la PLATA, no la logistica: por eso solo tiene los estados
 * del cobro. Que la mercaderia se despacho, esta en transito o llego son hechos
 * del envio, que se modela aparte y con su propio responsable.
 *
 * El flujo es PENDIENTE -> PAGADA, y desde PENDIENTE tambien se puede
 * CANCELAR. PAGADA y CANCELADA son finales, y en particular una orden ya
 * pagada NO se cancela: mientras no haya devolucion del dinero, cancelar un
 * cobro seria decir en la base algo que no paso en la realidad.
 *
 * Se guarda como texto gracias al @Enumerated(STRING) de OrdenDeCompra, asi que
 * agregar estados no rompe los datos existentes.
 */
public enum EstadoOrden {
    PENDIENTE,
    PAGADA,
    CANCELADA
}
