package com.uade.tpo.marketplace.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * Datos que entran por el body al crear o editar una categoria.
 *
 * Spring lo arma desde el JSON y viaja del controller al service. No se
 * persiste: CategoriaServiceImpl copia sus campos a la entidad Categoria.
 */
@Data
public class CategoriaRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @Size(max = 500, message = "La descripcion no puede superar los 500 caracteres")
    private String descripcion;

    /** Null para crear una categoria raiz. */
    private Long idCategoriaPadre;
}
