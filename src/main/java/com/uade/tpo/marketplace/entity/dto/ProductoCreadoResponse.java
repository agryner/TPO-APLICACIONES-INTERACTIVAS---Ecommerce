package com.uade.tpo.marketplace.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Respuesta del alta de un producto.
 *
 * Ademas del producto lleva un mensaje, porque el alta no termina el trabajo: el
 * producto queda en BORRADOR y recien se publica cuando le suben una foto. El
 * mensaje es para que el vendedor entienda por que todavia no lo ve en el
 * catalogo.
 */
@Data
@AllArgsConstructor
public class ProductoCreadoResponse {
    private ProductoResponse producto;
    private String mensaje;
}
