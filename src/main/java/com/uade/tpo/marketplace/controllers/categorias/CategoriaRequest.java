package com.uade.tpo.marketplace.controllers.categorias;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class CategoriaRequest {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @Size(max = 500, message = "La descripcion no puede superar los 500 caracteres")
    private String descripcion;

    private Long idCategoriaPadre;
}
