package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.entity.EstadoOrden;
import com.uade.tpo.marketplace.entity.dto.OrdenDeCompraResponse;
import com.uade.tpo.marketplace.entity.dto.RolEnOrden;
import java.util.List;

import com.uade.tpo.marketplace.exceptions.CambioDeEstadoNoPermitidoException;
import com.uade.tpo.marketplace.exceptions.CarritoVacioException;
import com.uade.tpo.marketplace.exceptions.OrdenNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.StockInsuficienteException;
import com.uade.tpo.marketplace.exceptions.TransicionInvalidaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;

/**
 * Contrato de la logica de ordenes.
 *
 * Lo consume OrdenesController y lo implementa OrdenDeCompraServiceImpl.
 */
public interface OrdenDeCompraService {

    /**
     * Las ordenes del solicitante y nada mas.
     *
     * Un cliente no ve las ordenes de otros: una orden es una transaccion
     * entre dos personas y a nadie mas le incumbe. Con rol en null vienen las
     * dos puntas juntas; con COMPRADOR o VENDEDOR se mira una sola.
     *
     * El ADMIN es la excepcion: sin rol ve todas las del sistema, para poder
     * auditarlas. Si manda rol vuelve a mirarse como participante y salen sus
     * propias compras o ventas, que es lo unico que ese filtro puede querer
     * decir.
     *
     * Tira 404 si el usuario no existe, para distinguirlo de un usuario real
     * que todavia no tiene movimientos: los dos casos darian una lista vacia.
     */
    List<OrdenDeCompraResponse> getOrdenes(Long idSolicitante, RolEnOrden rol)
            throws UsuarioNoEncontradoException;

    /** Una orden puntual. La ven su comprador, su vendedor y el ADMIN. */
    OrdenDeCompraResponse getOrdenById(Long idOrden, Long idSolicitante)
            throws OrdenNoEncontradaException, OperacionAjenaException;

    /**
     * Confirma el carrito del usuario y lo deja vacio.
     */
    /**
     * Cierra el carrito. Devuelve una orden por cada vendedor involucrado,
     * porque cada orden es una transaccion entre dos personas.
     */
    List<OrdenDeCompraResponse> createOrden(Long idSolicitante)
            throws UsuarioNoEncontradoException, CarritoVacioException, StockInsuficienteException,
            ProductoNoEncontradoException;

    /**
     * Avanza el estado de una orden. Solo el comprador o el vendedor pueden
     * pedirlo, y cada uno unicamente en los pasos que le corresponden.
     */
    OrdenDeCompraResponse actualizarEstado(Long idOrden, EstadoOrden estado, Long idSolicitante)
            throws OrdenNoEncontradaException, TransicionInvalidaException,
            CambioDeEstadoNoPermitidoException;
}
