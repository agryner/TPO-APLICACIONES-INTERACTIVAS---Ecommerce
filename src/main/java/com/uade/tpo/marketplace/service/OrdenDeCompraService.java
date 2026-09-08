package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.entity.EstadoOrden;
import com.uade.tpo.marketplace.controllers.ordenes.OrdenDeCompraResponse;
import com.uade.tpo.marketplace.controllers.ordenes.RolEnOrden;
import java.util.List;

import com.uade.tpo.marketplace.exceptions.CambioDeEstadoNoPermitidoException;
import com.uade.tpo.marketplace.exceptions.CarritoVacioException;
import com.uade.tpo.marketplace.exceptions.CompraPropiaException;
import com.uade.tpo.marketplace.exceptions.OrdenNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.StockInsuficienteException;
import com.uade.tpo.marketplace.exceptions.TransicionInvalidaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.AdminNoComerciaException;

/**
 * Contrato de la logica de ordenes.
 *
 * Lo consume OrdenesController y lo implementa OrdenDeCompraServiceImpl.
 */
public interface OrdenDeCompraService {

    List<OrdenDeCompraResponse> getOrdenes(Long idSolicitante, RolEnOrden rol)
            throws UsuarioNoEncontradoException;

    OrdenDeCompraResponse getOrdenById(Long idOrden, Long idSolicitante)
            throws OrdenNoEncontradaException, OperacionAjenaException;

    List<OrdenDeCompraResponse> createOrden(Long idSolicitante)
            throws UsuarioNoEncontradoException, CarritoVacioException, StockInsuficienteException,
            ProductoNoEncontradoException, CompraPropiaException, CuentaInactivaException, AdminNoComerciaException;

    OrdenDeCompraResponse actualizarEstado(Long idOrden, EstadoOrden estado, Long idSolicitante)
            throws OrdenNoEncontradaException, TransicionInvalidaException,
            CambioDeEstadoNoPermitidoException;
}
