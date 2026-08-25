package com.uade.tpo.marketplace.entity.dto;

import lombok.Data;

@Data
public class CategoriaRequest {
    private String nombre;
    private String descripcion;

    /** Null para crear una categoria raiz. */
    private Long idCategoriaPadre;
}
