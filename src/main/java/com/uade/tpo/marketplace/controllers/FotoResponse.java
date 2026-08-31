package com.uade.tpo.marketplace.controllers;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Devuelve la imagen embebida en el JSON, codificada en Base64.
 * Sirve para clientes que no pueden hacer una segunda request al endpoint
 * de contenido binario.
 *
 * Lo arma FotosController con los bytes que le da FotoService.
 */
@Data
@AllArgsConstructor
public class FotoResponse {
    private Long id;
    private String archivo;
}
