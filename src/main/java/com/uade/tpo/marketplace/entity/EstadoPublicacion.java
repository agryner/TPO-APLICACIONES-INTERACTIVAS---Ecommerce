package com.uade.tpo.marketplace.entity;

/**
 * En que punto de la carga esta una publicacion.
 *
 * El alta de un producto lo deja en BORRADOR: existe y tiene sus datos, pero no
 * aparece en el catalogo. Pasa a PUBLICADO cuando se le sube la primera foto, y
 * vuelve a BORRADOR si se queda sin ninguna. Asi el catalogo nunca muestra un
 * producto sin imagen, sin necesidad de pedir la foto en el mismo request que
 * los datos, que es imposible: la foto necesita el id del producto.
 *
 * PAUSADO lo decide el vendedor cuando quiere sacar la publicacion de circulacion
 * sin darla de baja: no se lista ni se puede comprar, pero conserva sus datos y
 * sus fotos, y vuelve al catalogo cuando la reanuda.
 */
public enum EstadoPublicacion {

    /** Cargado pero todavia sin fotos. Solo lo ve su vendedor. */
    BORRADOR,

    /** Tiene al menos una foto y se muestra en el catalogo. */
    PUBLICADO,

    /** El vendedor la saco de circulacion. Se puede reanudar cuando quiera. */
    PAUSADO
}
