package com.uade.tpo.marketplace.entity.dto;

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
public class FotoContenidoResponse {
    private Long id;
    private String archivo;
}
