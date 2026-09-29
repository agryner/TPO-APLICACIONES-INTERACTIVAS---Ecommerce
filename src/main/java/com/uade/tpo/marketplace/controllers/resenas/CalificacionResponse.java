package com.uade.tpo.marketplace.controllers.resenas;

import lombok.AllArgsConstructor;
import com.uade.tpo.marketplace.entity.NivelVendedor;

import lombok.Data;

@Data
@AllArgsConstructor
public class CalificacionResponse {
    private String nombreUsuario;
    private Double promedio;
    private long cantidad;
    private NivelVendedor nivel;
}
