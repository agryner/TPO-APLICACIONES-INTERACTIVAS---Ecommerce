package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.carritos.CarritoResponse;
import com.uade.tpo.marketplace.controllers.carritos.ItemCarritoRequest;
import com.uade.tpo.marketplace.entity.Carrito;
import com.uade.tpo.marketplace.exceptions.ItemCarritoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.StockInsuficienteException;
import com.uade.tpo.marketplace.exceptions.CompraPropiaException;
import com.uade.tpo.marketplace.exceptions.CantidadInvalidaException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.AdminNoComerciaException;

/**
 * Contrato de la logica del carrito.
 *
 * Lo consume CarritosController y lo implementa CarritoServiceImpl.
 */
public interface CarritoService {

    CarritoResponse obtenerCarrito(Long idUsuario) throws UsuarioNoEncontradoException, CuentaInactivaException;

    Carrito obtenerCarritoEntidad(Long idUsuario) throws UsuarioNoEncontradoException;

    void quitarDeTodosLosCarritos(Long idProducto);

    void vaciarEntidad(Long idUsuario) throws UsuarioNoEncontradoException;

    CarritoResponse agregarItem(Long idUsuario, ItemCarritoRequest request)
            throws UsuarioNoEncontradoException,
            ProductoNoEncontradoException, StockInsuficienteException, CompraPropiaException,
            CantidadInvalidaException, CuentaInactivaException, AdminNoComerciaException;

    CarritoResponse modificarCantidad(Long idUsuario, Long idItem, Integer nuevaCantidad)
            throws UsuarioNoEncontradoException, ItemCarritoNoEncontradoException,
            StockInsuficienteException, CuentaInactivaException;

    CarritoResponse eliminarItem(Long idUsuario, Long idItem)
            throws UsuarioNoEncontradoException, ItemCarritoNoEncontradoException, CuentaInactivaException;

    CarritoResponse vaciar(Long idUsuario) throws UsuarioNoEncontradoException, CuentaInactivaException;
}
