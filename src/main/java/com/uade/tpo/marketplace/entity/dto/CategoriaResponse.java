package com.uade.tpo.marketplace.entity.dto;

import com.uade.tpo.marketplace.entity.Categoria;

import lombok.Data;

/**
 * Vista publica de una categoria.
 *
 * Del padre expone solo id y nombre en vez de anidar la categoria entera: la
 * jerarquia no tiene profundidad limitada y anidarla haria crecer el JSON sin
 * control. Las subcategorias se piden aparte, en /categorias/{id}/subcategorias.
 */
@Data
public class CategoriaResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private Long idCategoriaPadre;
    private String nombreCategoriaPadre;

    public static CategoriaResponse from(Categoria categoria) {
        if (categoria == null)
            return null;

        CategoriaResponse dto = new CategoriaResponse();
        dto.setId(categoria.getId());
        dto.setNombre(categoria.getNombre());
        dto.setDescripcion(categoria.getDescripcion());

        Categoria padre = categoria.getCategoriaPadre();
        if (padre != null) {
            dto.setIdCategoriaPadre(padre.getId());
            dto.setNombreCategoriaPadre(padre.getNombre());
        }
        return dto;
    }
}
