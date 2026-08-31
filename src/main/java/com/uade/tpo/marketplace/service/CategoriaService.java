package com.uade.tpo.marketplace.service;

import java.util.List;
import java.util.Optional;

import com.uade.tpo.marketplace.entity.Categoria;
import com.uade.tpo.marketplace.controllers.CategoriaRequest;
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

    List<Categoria> getCategorias();

    /** Solo las categorias sin padre. */
    List<Categoria> getCategoriasRaiz();

    /** Las hijas directas de una categoria. */
    List<Categoria> getSubcategorias(Long idCategoria) throws CategoriaNoEncontradaException;

    Optional<Categoria> getCategoriaById(Long idCategoria);

    /** Alta de categoria. Solo la puede ejecutar un usuario con rol ADMIN. */
    Categoria createCategoria(CategoriaRequest request, Long idUsuario)
            throws CategoriaDuplicadaException, CategoriaNoEncontradaException,
            UsuarioNoEncontradoException, AccesoDenegadoException;

    /** Edicion de categoria. Solo la puede ejecutar un usuario con rol ADMIN. */
    Categoria updateCategoria(Long idCategoria, CategoriaRequest request, Long idUsuario)
            throws CategoriaNoEncontradaException, JerarquiaInvalidaException,
            UsuarioNoEncontradoException, AccesoDenegadoException;

    /** Baja de categoria. Solo la puede ejecutar un usuario con rol ADMIN. */
    void deleteCategoria(Long idCategoria, Long idUsuario)
            throws CategoriaNoEncontradaException, CategoriaConSubcategoriasException,
            UsuarioNoEncontradoException, AccesoDenegadoException;
}
