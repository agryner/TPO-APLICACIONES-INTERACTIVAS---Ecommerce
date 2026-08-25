package com.uade.tpo.marketplace.service;

import java.util.List;
import java.util.Optional;

import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.entity.dto.ProductoRequest;
import com.uade.tpo.marketplace.exceptions.CategoriaNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;

public interface ProductoService {

    List<Producto> getProductos(Long idCategoria, Long idVendedor, String nombre);

    Optional<Producto> getProductoById(Long idProducto);

    Producto createProducto(ProductoRequest request)
            throws CategoriaNoEncontradaException, UsuarioNoEncontradoException;

    Producto updateProducto(Long idProducto, ProductoRequest request)
            throws ProductoNoEncontradoException, CategoriaNoEncontradaException, UsuarioNoEncontradoException;

    void deleteProducto(Long idProducto) throws ProductoNoEncontradoException;
}
