package com.uade.tpo.marketplace.controllers.categorias;

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

    /**
     * Traduce la entidad al objeto que sale por HTTP.
     *
     * Pre : la entidad Categoria, o null.
     * Post: el DTO con el id y el nombre del padre resueltos, en vez del
     *       objeto anidado.
     */
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
