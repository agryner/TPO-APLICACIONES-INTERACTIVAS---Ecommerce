package com.uade.tpo.marketplace.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.uade.tpo.marketplace.controllers.ProductoRequest;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.exceptions.CategoriaNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;

/**
 * Contrato de la logica de productos.
 *
 * Lo consume ProductosController y lo implementa ProductoServiceImpl.
 */
public interface ProductoService {

    List<Producto> getProductos(Long idCategoria, Long idVendedor, String nombre, BigDecimal precioMin, BigDecimal precioMax);

    Optional<Producto> getProductoById(Long idProducto);

    Producto createProducto(ProductoRequest request)
            throws CategoriaNoEncontradaException, UsuarioNoEncontradoException;

    Producto updateProducto(Long idProducto, ProductoRequest request)
            throws ProductoNoEncontradoException, CategoriaNoEncontradaException, UsuarioNoEncontradoException;

    void deleteProducto(Long idProducto) throws ProductoNoEncontradoException;
}
