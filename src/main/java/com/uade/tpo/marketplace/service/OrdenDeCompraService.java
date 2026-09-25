package com.uade.tpo.marketplace.service;

import java.math.BigDecimal;

import com.uade.tpo.marketplace.entity.DireccionEntrega;
import com.uade.tpo.marketplace.entity.EstadoOrden;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.controllers.ordenes.OrdenDeCompraResponse;
import com.uade.tpo.marketplace.controllers.ordenes.OrdenRequest;
import com.uade.tpo.marketplace.controllers.ordenes.RolEnOrden;
import java.util.List;

import com.uade.tpo.marketplace.exceptions.CambioDeEstadoNoPermitidoException;
import com.uade.tpo.marketplace.exceptions.CarritoVacioException;
import com.uade.tpo.marketplace.exceptions.CompraPropiaException;
import com.uade.tpo.marketplace.exceptions.DireccionDeEntregaRequeridaException;
import com.uade.tpo.marketplace.exceptions.OrdenNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.StockInsuficienteException;
import com.uade.tpo.marketplace.exceptions.TransicionInvalidaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.RolNoComerciaException;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;

public interface OrdenDeCompraService {
    List<OrdenDeCompraResponse> getOrdenes(Long idSolicitante, RolEnOrden rol)
            throws UsuarioNoEncontradoException, SinResultadosException;

    OrdenDeCompraResponse getOrdenById(Long idOrden, Long idSolicitante)
            throws OrdenNoEncontradaException, OperacionAjenaException;

    OrdenDeCompraResponse crearDesdeOferta(Usuario comprador, Producto producto, int cantidad,
            BigDecimal precioAcordado, DireccionEntrega entrega)
            throws StockInsuficienteException;

    List<OrdenDeCompraResponse> createOrden(Long idSolicitante, OrdenRequest request)
            throws UsuarioNoEncontradoException, CarritoVacioException, StockInsuficienteException,
            ProductoNoEncontradoException, CompraPropiaException, CuentaInactivaException,
            RolNoComerciaException, DireccionDeEntregaRequeridaException;

    OrdenDeCompraResponse actualizarEstado(Long idOrden, EstadoOrden estado, Long idSolicitante)
            throws OrdenNoEncontradaException, TransicionInvalidaException,
            CambioDeEstadoNoPermitidoException;
}
