package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.productos.FiltroProductos;
import com.uade.tpo.marketplace.controllers.productos.ProductoRequest;
import com.uade.tpo.marketplace.entity.EstadoPublicacion;
import com.uade.tpo.marketplace.controllers.productos.ProductoCreadoResponse;
import com.uade.tpo.marketplace.controllers.productos.ProductoResponse;
import com.uade.tpo.marketplace.controllers.productos.ProductoResumenResponse;
import java.math.BigDecimal;
import java.util.List;

import com.uade.tpo.marketplace.exceptions.AnioInvalidoException;
import com.uade.tpo.marketplace.exceptions.CategoriaNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.OrdenamientoInvalidoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.TransicionInvalidaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.RolNoComerciaException;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;

public interface ProductoService {
    List<ProductoResumenResponse> getProductos(FiltroProductos filtro)
            throws OrdenamientoInvalidoException, SinResultadosException;

    List<ProductoResumenResponse> getSimilares(Long idProducto)
            throws ProductoNoEncontradoException, SinResultadosException;

    ProductoResponse getProductoById(Long idProducto, Long idSolicitante)
            throws ProductoNoEncontradoException;

    List<ProductoResumenResponse> getPublicacionesDeVendedor(String nombreUsuario)
            throws UsuarioNoEncontradoException, SinResultadosException;

    List<ProductoResponse> getMisPublicaciones(Long idSolicitante, EstadoPublicacion estado)
            throws UsuarioNoEncontradoException, RolNoComerciaException, SinResultadosException;

    ProductoCreadoResponse createProducto(ProductoRequest request, Long idSolicitante)
            throws CategoriaNoEncontradaException, UsuarioNoEncontradoException,
            CuentaInactivaException, RolNoComerciaException, AnioInvalidoException;

    ProductoResponse updateProducto(Long idProducto, ProductoRequest request, Long idSolicitante)
            throws ProductoNoEncontradoException, CategoriaNoEncontradaException,
            UsuarioNoEncontradoException, OperacionAjenaException, CuentaInactivaException,
            AnioInvalidoException;

    ProductoResponse cambiarEstadoPublicacion(Long idProducto, EstadoPublicacion estado,
            Long idSolicitante)
            throws ProductoNoEncontradoException, OperacionAjenaException,
            TransicionInvalidaException, CuentaInactivaException, UsuarioNoEncontradoException;

    ProductoResponse reactivarProducto(Long idProducto, Long idSolicitante)
            throws ProductoNoEncontradoException, OperacionAjenaException, CuentaInactivaException,
            UsuarioNoEncontradoException;

    List<ProductoResponse> getTodosLosProductos(Long idSolicitante, EstadoPublicacion estado)
            throws UsuarioNoEncontradoException, AccesoDenegadoException, SinResultadosException;

    void deleteProducto(Long idProducto, Long idSolicitante)
            throws ProductoNoEncontradoException, OperacionAjenaException, CuentaInactivaException, UsuarioNoEncontradoException;
}
