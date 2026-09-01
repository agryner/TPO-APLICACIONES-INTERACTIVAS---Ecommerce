package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.ItemCarritoRequest;
import com.uade.tpo.marketplace.entity.Carrito;
import com.uade.tpo.marketplace.exceptions.ItemCarritoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.StockInsuficienteException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;

/**
 * Contrato de la logica del carrito.
 *
 * Lo consume CarritosController y lo implementa CarritoServiceImpl.
 */
public interface CarritoService {

    /**
     * Devuelve el carrito del usuario. Si nunca tuvo uno, se lo crea vacio.
     * Si el carrito venció, se devuelve vacio.
     */
    Carrito obtenerCarrito(Long idUsuario) throws UsuarioNoEncontradoException;

    Carrito agregarItem(Long idUsuario, ItemCarritoRequest request)
            throws UsuarioNoEncontradoException, ProductoNoEncontradoException, StockInsuficienteException;

    Carrito modificarCantidad(Long idUsuario, Long idItem, Integer nuevaCantidad)
            throws UsuarioNoEncontradoException, ItemCarritoNoEncontradoException, StockInsuficienteException;

    Carrito eliminarItem(Long idUsuario, Long idItem)
            throws UsuarioNoEncontradoException, ItemCarritoNoEncontradoException;

    Carrito vaciar(Long idUsuario) throws UsuarioNoEncontradoException;
}
