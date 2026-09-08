package com.uade.tpo.marketplace.controllers.productos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductoCreadoResponse {
    private ProductoResponse producto;
    private String mensaje;
}
