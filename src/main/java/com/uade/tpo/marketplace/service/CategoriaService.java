package com.uade.tpo.marketplace.service;

import java.util.List;
import java.util.Optional;

import com.uade.tpo.marketplace.entity.Categoria;
import com.uade.tpo.marketplace.entity.dto.CategoriaRequest;
import com.uade.tpo.marketplace.exceptions.CategoriaConSubcategoriasException;
import com.uade.tpo.marketplace.exceptions.CategoriaDuplicadaException;
import com.uade.tpo.marketplace.exceptions.CategoriaNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.JerarquiaInvalidaException;

public interface CategoriaService {

    List<Categoria> getCategorias();

    /** Solo las categorias sin padre. */
    List<Categoria> getCategoriasRaiz();

    /** Las hijas directas de una categoria. */
    List<Categoria> getSubcategorias(Long idCategoria) throws CategoriaNoEncontradaException;

    Optional<Categoria> getCategoriaById(Long idCategoria);

    Categoria createCategoria(CategoriaRequest request)
            throws CategoriaDuplicadaException, CategoriaNoEncontradaException;

    Categoria updateCategoria(Long idCategoria, CategoriaRequest request)
            throws CategoriaNoEncontradaException, JerarquiaInvalidaException;

    void deleteCategoria(Long idCategoria)
            throws CategoriaNoEncontradaException, CategoriaConSubcategoriasException;
}
