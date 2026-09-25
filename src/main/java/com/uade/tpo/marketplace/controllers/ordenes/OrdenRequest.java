package com.uade.tpo.marketplace.controllers.ordenes;

import com.uade.tpo.marketplace.entity.Provincia;

import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class OrdenRequest {
    private Boolean coordinarConVendedor;

    private Provincia provinciaEntrega;

    @Size(max = 100, message = "La localidad no puede superar los 100 caracteres")
    private String localidadEntrega;

    @Size(max = 150, message = "La calle no puede superar los 150 caracteres")
    private String direccionEntrega;
}
