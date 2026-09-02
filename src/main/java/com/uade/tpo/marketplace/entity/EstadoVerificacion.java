package com.uade.tpo.marketplace.entity;

/**
 * Resultado de la verificacion automatica de una foto.
 *
 * No existe un estado RECHAZADA porque una foto rechazada nunca llega a
 * guardarse: la subida se corta con un error y el vendedor manda otra.
 */
public enum EstadoVerificacion {

    /** La IA confirmo que la foto se corresponde con la categoria. */
    APROBADA,

    /** Quedo en duda, o la IA no respondio. La mira un administrador. */
    EN_REVISION
}
