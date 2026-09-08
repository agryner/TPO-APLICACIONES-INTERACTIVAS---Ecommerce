package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.productos.ProductoRequest;
import com.uade.tpo.marketplace.entity.EstadoPublicacion;
import com.uade.tpo.marketplace.controllers.productos.ProductoCreadoResponse;
import com.uade.tpo.marketplace.controllers.productos.ProductoResponse;
import java.math.BigDecimal;
import java.util.List;

import com.uade.tpo.marketplace.exceptions.CategoriaNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.OrdenamientoInvalidoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.TransicionInvalidaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.AdminNoComerciaException;

/**
 * Contrato de la logica de productos.
 *
 * Lo consume ProductosController y lo implementa ProductoServiceImpl.
 */
public interface ProductoService {

    List<ProductoResponse> getProductos(Long idCategoria, String nombre,
            BigDecimal precioMin, BigDecimal precioMax, String ordenPrecio)
            throws OrdenamientoInvalidoException;

    ProductoResponse getProductoById(Long idProducto) throws ProductoNoEncontradoException;

    List<ProductoResponse> getPublicacionesDeVendedor(String nombreUsuario)
            throws UsuarioNoEncontradoException;

    List<ProductoResponse> getMisPublicaciones(Long idSolicitante, EstadoPublicacion estado)
            throws UsuarioNoEncontradoException, AdminNoComerciaException;

    ProductoCreadoResponse createProducto(ProductoRequest request, Long idSolicitante)
            throws CategoriaNoEncontradaException, UsuarioNoEncontradoException, CuentaInactivaException, AdminNoComerciaException;

    ProductoResponse updateProducto(Long idProducto, ProductoRequest request, Long idSolicitante)
            throws ProductoNoEncontradoException, CategoriaNoEncontradaException,
            UsuarioNoEncontradoException, OperacionAjenaException, CuentaInactivaException;

    ProductoResponse cambiarEstadoPublicacion(Long idProducto, EstadoPublicacion estado,
            Long idSolicitante)
            throws ProductoNoEncontradoException, OperacionAjenaException,
            TransicionInvalidaException, CuentaInactivaException, UsuarioNoEncontradoException;

    ProductoResponse reactivarProducto(Long idProducto, Long idSolicitante)
            throws ProductoNoEncontradoException, OperacionAjenaException, CuentaInactivaException,
            UsuarioNoEncontradoException;

    List<ProductoResponse> getTodosLosProductos(Long idSolicitante, EstadoPublicacion estado)
            throws UsuarioNoEncontradoException, AccesoDenegadoException;

    void deleteProducto(Long idProducto, Long idSolicitante)
            throws ProductoNoEncontradoException, OperacionAjenaException, CuentaInactivaException, UsuarioNoEncontradoException;
}
