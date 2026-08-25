package com.uade.tpo.marketplace.service;

import java.util.List;
import java.util.Optional;

import com.uade.tpo.marketplace.entity.OrdenDeCompra;
import com.uade.tpo.marketplace.entity.dto.OrdenDeCompraRequest;
import com.uade.tpo.marketplace.exceptions.CarritoVacioException;
import com.uade.tpo.marketplace.exceptions.OrdenNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.StockInsuficienteException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;

public interface OrdenDeCompraService {

    List<OrdenDeCompra> getOrdenes();

    List<OrdenDeCompra> getOrdenesByUsuario(Long idUsuario);

    Optional<OrdenDeCompra> getOrdenById(Long idOrden);

    /**
     * Confirma el carrito del usuario y lo deja vacio.
     */
    OrdenDeCompra createOrden(OrdenDeCompraRequest request)
            throws UsuarioNoEncontradoException, CarritoVacioException, StockInsuficienteException;

    OrdenDeCompra actualizarEstado(Long idOrden, String estado) throws OrdenNoEncontradaException;
}
