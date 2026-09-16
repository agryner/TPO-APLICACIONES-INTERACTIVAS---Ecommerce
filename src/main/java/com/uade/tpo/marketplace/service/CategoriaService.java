package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.categorias.CategoriaRequest;
import com.uade.tpo.marketplace.controllers.categorias.CategoriaResponse;
import java.util.List;

import com.uade.tpo.marketplace.exceptions.CategoriaConProductosException;
import com.uade.tpo.marketplace.exceptions.CategoriaConSubcategoriasException;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.CategoriaDuplicadaException;
import com.uade.tpo.marketplace.exceptions.CategoriaNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.CategoriaPadreInactivaException;
import com.uade.tpo.marketplace.exceptions.JerarquiaInvalidaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;

public interface CategoriaService {
    List<CategoriaResponse> getCategorias() throws SinResultadosException;

    List<CategoriaResponse> getCategoriasRaiz() throws SinResultadosException;

    List<CategoriaResponse> getSubcategorias(Long idCategoria)
            throws CategoriaNoEncontradaException, SinResultadosException;

    CategoriaResponse getCategoriaById(Long idCategoria) throws CategoriaNoEncontradaException;

    CategoriaResponse createCategoria(CategoriaRequest request, Long idSolicitante)
            throws CategoriaDuplicadaException, CategoriaNoEncontradaException,
            UsuarioNoEncontradoException, AccesoDenegadoException;

    CategoriaResponse updateCategoria(Long idCategoria, CategoriaRequest request, Long idSolicitante)
            throws CategoriaNoEncontradaException, JerarquiaInvalidaException,
            CategoriaDuplicadaException, UsuarioNoEncontradoException, AccesoDenegadoException;

    CategoriaResponse reactivarCategoria(Long idCategoria, Long idSolicitante)
            throws CategoriaNoEncontradaException, CategoriaDuplicadaException,
            CategoriaPadreInactivaException, UsuarioNoEncontradoException,
            AccesoDenegadoException;

    void deleteCategoria(Long idCategoria, Long idSolicitante)
            throws CategoriaNoEncontradaException, CategoriaConSubcategoriasException,
            CategoriaConProductosException, UsuarioNoEncontradoException,
            AccesoDenegadoException;
}
