package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.categorias.CategoriaRequest;
import com.uade.tpo.marketplace.controllers.categorias.CategoriaResponse;
import java.util.List;

import com.uade.tpo.marketplace.exceptions.CategoriaConProductosException;
import com.uade.tpo.marketplace.exceptions.CategoriaConSubcategoriasException;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.CategoriaDuplicadaException;
import com.uade.tpo.marketplace.exceptions.CategoriaNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.JerarquiaInvalidaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;

/**
 * Contrato de la logica de categorias.
 *
 * Lo consume CategoriasController y lo implementa CategoriaServiceImpl. La
 * interfaz existe para que el controller no dependa de la implementacion.
 */
public interface CategoriaService {

    List<CategoriaResponse> getCategorias();

    List<CategoriaResponse> getCategoriasRaiz();

    List<CategoriaResponse> getSubcategorias(Long idCategoria) throws CategoriaNoEncontradaException;

    CategoriaResponse getCategoriaById(Long idCategoria) throws CategoriaNoEncontradaException;

    CategoriaResponse createCategoria(CategoriaRequest request, Long idSolicitante)
            throws CategoriaDuplicadaException, CategoriaNoEncontradaException,
            UsuarioNoEncontradoException, AccesoDenegadoException;

    CategoriaResponse updateCategoria(Long idCategoria, CategoriaRequest request, Long idSolicitante)
            throws CategoriaNoEncontradaException, JerarquiaInvalidaException,
            CategoriaDuplicadaException, UsuarioNoEncontradoException, AccesoDenegadoException;

    void deleteCategoria(Long idCategoria, Long idSolicitante)
            throws CategoriaNoEncontradaException, CategoriaConSubcategoriasException,
            CategoriaConProductosException, UsuarioNoEncontradoException,
            AccesoDenegadoException;
}
