package com.uade.tpo.marketplace.entity.dto;

/**
 * Desde que lado se mira una orden.
 *
 * No es un rol del usuario ni se guarda en ningun lado: la misma persona es
 * comprador en unas ordenes y vendedor en otras. Sirve solo para filtrar el
 * listado propio.
 */
public enum RolEnOrden {
    COMPRADOR,
    VENDEDOR
}
