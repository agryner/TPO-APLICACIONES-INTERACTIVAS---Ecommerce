package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.entity.Carrito;
import com.uade.tpo.marketplace.entity.dto.ItemCarritoRequest;
import com.uade.tpo.marketplace.exceptions.ItemCarritoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.StockInsuficienteException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;

public interface CarritoService {

    /**
     * Devuelve el carrito del usuario. Si nunca tuvo uno, se lo crea vacio.
     * Si el carrito venció, se devuelve vacio.
     */
    Carrito obtenerCarrito(Long idUsuario) throws UsuarioNoEncontradoException;

    Carrito agregarItem(Long idUsuario, ItemCarritoRequest request)
            throws UsuarioNoEncontradoException, ProductoNoEncontradoException, StockInsuficienteException;

    Carrito eliminarItem(Long idUsuario, Long idItem)
            throws UsuarioNoEncontradoException, ItemCarritoNoEncontradoException;

    Carrito vaciar(Long idUsuario) throws UsuarioNoEncontradoException;
}
