package com.uade.tpo.marketplace.controllers;

import lombok.Data;

/**
 * Datos que entran por el body al crear o editar una categoria.
 *
 * Spring lo arma desde el JSON y viaja del controller al service. No se
 * persiste: CategoriaServiceImpl copia sus campos a la entidad Categoria.
 */
@Data
public class CategoriaRequest {
    private String nombre;
    private String descripcion;

    /** Null para crear una categoria raiz. */
    private Long idCategoriaPadre;
}
