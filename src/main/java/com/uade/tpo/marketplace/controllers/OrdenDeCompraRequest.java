package com.uade.tpo.marketplace.controllers;

import lombok.Data;

/**
 * Datos que entran por el body al generar una orden.
 *
 * Solo identifica al usuario: los renglones los arma el service a partir de
 * lo que ese usuario tenga en el carrito.
 */
@Data
public class OrdenDeCompraRequest {
    private Long idUsuario;
}
