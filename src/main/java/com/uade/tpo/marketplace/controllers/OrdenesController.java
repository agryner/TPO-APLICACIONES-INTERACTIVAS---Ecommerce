package com.uade.tpo.marketplace.controllers;

import com.uade.tpo.marketplace.entity.EstadoOrden;
import com.uade.tpo.marketplace.entity.dto.OrdenDeCompraResponse;
import com.uade.tpo.marketplace.entity.dto.RolEnOrden;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.exceptions.CambioDeEstadoNoPermitidoException;
import com.uade.tpo.marketplace.exceptions.CarritoVacioException;
import com.uade.tpo.marketplace.exceptions.OrdenNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.StockInsuficienteException;
import com.uade.tpo.marketplace.exceptions.TransicionInvalidaException;
import com.uade.tpo.marketplace.service.OrdenDeCompraService;

import lombok.RequiredArgsConstructor;

/**
 * Endpoints REST de ordenes de compra.
 *
 * Delega en OrdenDeCompraService y devuelve OrdenDeCompraResponse. Todo lo
 * que se lee pasa por idSolicitante: cada uno ve unicamente las ordenes en las
 * que participa. El alta toma el contenido del carrito del usuario, asi que el
 * body solo trae de quien es la orden.
 */
@RestController
@RequestMapping("ordenes")
@RequiredArgsConstructor
public class OrdenesController {

    private final OrdenDeCompraService ordenService;

    @GetMapping
    /**
     * idSolicitante es obligatorio: sin decir quien pregunta no se lista nada.
     * rol es opcional y elige que punta mirar; si no viene, un cliente recibe
     * sus compras y sus ventas juntas y un ADMIN todas las del sistema. Al ser
     * enum, Spring rechaza con 400 cualquier valor que no sea COMPRADOR o
     * VENDEDOR.
     */
    public ResponseEntity<List<OrdenDeCompraResponse>> getOrdenes(
            @RequestParam Long idSolicitante,
            @RequestParam(required = false) RolEnOrden rol)
            throws UsuarioNoEncontradoException {
        return ResponseEntity.ok(ordenService.getOrdenes(idSolicitante, rol));
    }

    @GetMapping("/{idOrden}")
    public ResponseEntity<OrdenDeCompraResponse> getOrdenById(@PathVariable Long idOrden,
            @RequestParam Long idSolicitante)
            throws OrdenNoEncontradaException, OperacionAjenaException {
        return ResponseEntity.ok(ordenService.getOrdenById(idOrden, idSolicitante));
    }

    /**
     * Devuelve una lista porque un carrito con productos de varios vendedores
     * se cierra como varias ordenes, una por cada uno.
     */
    @PostMapping
    public ResponseEntity<List<OrdenDeCompraResponse>> createOrden(
            @RequestParam Long idSolicitante)
            throws UsuarioNoEncontradoException, CarritoVacioException, StockInsuficienteException,
            ProductoNoEncontradoException {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordenService.createOrden(idSolicitante));
    }


    @PutMapping("/{idOrden}/estado")
    /**
     * El estado llega como enum, asi que Spring rechaza con 400 cualquier valor
     * que no exista sin que haya que validarlo a mano.
     */
    public ResponseEntity<OrdenDeCompraResponse> actualizarEstado(@PathVariable Long idOrden,
            @RequestParam EstadoOrden estado, @RequestParam Long idSolicitante)
            throws OrdenNoEncontradaException, TransicionInvalidaException,
            CambioDeEstadoNoPermitidoException {
        return ResponseEntity.ok(ordenService.actualizarEstado(idOrden, estado, idSolicitante));
    }
}
