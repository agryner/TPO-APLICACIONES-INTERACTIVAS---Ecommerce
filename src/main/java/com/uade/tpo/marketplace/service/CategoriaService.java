package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.entity.dto.CategoriaRequest;
import com.uade.tpo.marketplace.entity.dto.CategoriaResponse;
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

    /** Solo las categorias sin padre. */
    List<CategoriaResponse> getCategoriasRaiz();

    /** Las hijas directas de una categoria. */
    List<CategoriaResponse> getSubcategorias(Long idCategoria) throws CategoriaNoEncontradaException;

    CategoriaResponse getCategoriaById(Long idCategoria) throws CategoriaNoEncontradaException;

    /** Alta de categoria. Solo la puede ejecutar un usuario con rol ADMIN. */
    CategoriaResponse createCategoria(CategoriaRequest request, Long idSolicitante)
            throws CategoriaDuplicadaException, CategoriaNoEncontradaException,
            UsuarioNoEncontradoException, AccesoDenegadoException;

    /** Edicion de categoria. Solo la puede ejecutar un usuario con rol ADMIN. */
    CategoriaResponse updateCategoria(Long idCategoria, CategoriaRequest request, Long idSolicitante)
            throws CategoriaNoEncontradaException, JerarquiaInvalidaException,
            CategoriaDuplicadaException, UsuarioNoEncontradoException, AccesoDenegadoException;

    /** Baja de categoria. Solo la puede ejecutar un usuario con rol ADMIN. */
    void deleteCategoria(Long idCategoria, Long idSolicitante)
            throws CategoriaNoEncontradaException, CategoriaConSubcategoriasException,
            CategoriaConProductosException, UsuarioNoEncontradoException,
            AccesoDenegadoException;
}
