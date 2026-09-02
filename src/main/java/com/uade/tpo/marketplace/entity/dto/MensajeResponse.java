package com.uade.tpo.marketplace.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Respuesta simple para las operaciones que no devuelven un recurso,
 * como los borrados.
 *
 * Lo arma el controller al terminar; los services no lo conocen.
 */
@Data
@AllArgsConstructor
public class MensajeResponse {
    private String mensaje;
}
