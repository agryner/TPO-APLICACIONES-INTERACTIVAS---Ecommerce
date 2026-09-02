package com.uade.tpo.marketplace.controllers;

import com.uade.tpo.marketplace.entity.dto.MensajeResponse;
import com.uade.tpo.marketplace.entity.dto.ProductoRequest;
import com.uade.tpo.marketplace.entity.EstadoPublicacion;
import com.uade.tpo.marketplace.entity.dto.ProductoCreadoResponse;
import com.uade.tpo.marketplace.entity.dto.ProductoResponse;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.exceptions.CategoriaNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.OrdenamientoInvalidoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.TransicionInvalidaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.service.ProductoService;

import lombok.RequiredArgsConstructor;

/**
 * Endpoints REST del catalogo de productos.
 *
 * Recibe ProductoRequest, delega en ProductoService y devuelve
 * ProductoResponse o MensajeResponse. Los filtros de busqueda llegan como query
 * params y se pasan tal cual al service.
 */
@RestController
@RequestMapping("productos")
@RequiredArgsConstructor
public class ProductosController {

    private final ProductoService productoService;

    @GetMapping
    public ResponseEntity<List<ProductoResponse>> getProductos(
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(required = false) String vendedor,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(required = false) String ordenPrecio)
            throws OrdenamientoInvalidoException {
        return ResponseEntity.ok(productoService.getProductos(
                idCategoria, vendedor, nombre, precioMin, precioMax, ordenPrecio));
    }

    /**
     * Las publicaciones propias, incluidos borradores y pausadas.
     *
     * Va antes que /{idProducto} porque Spring resuelve primero los segmentos
     * literales, pero conviene tenerlas juntas para que se vea el orden.
     */
    @GetMapping("/mis-publicaciones")
    public ResponseEntity<List<ProductoResponse>> getMisPublicaciones(
            @RequestParam Long idSolicitante,
            @RequestParam(required = false) EstadoPublicacion estado) {
        return ResponseEntity.ok(productoService.getMisPublicaciones(idSolicitante, estado));
    }

    @GetMapping("/{idProducto}")
    public ResponseEntity<ProductoResponse> getProductoById(@PathVariable Long idProducto)
            throws ProductoNoEncontradoException {
        return ResponseEntity.ok(productoService.getProductoById(idProducto));
    }

    @PostMapping
    public ResponseEntity<Object> createProducto(@RequestBody ProductoRequest request,
            @RequestParam Long idSolicitante)
            throws CategoriaNoEncontradaException, UsuarioNoEncontradoException {
        ProductoCreadoResponse result = productoService.createProducto(request, idSolicitante);
        return ResponseEntity.created(URI.create("/productos/" + result.getProducto().getId()))
                .body(result);
    }

    @PutMapping("/{idProducto}")
    public ResponseEntity<ProductoResponse> updateProducto(@PathVariable Long idProducto,
            @RequestBody ProductoRequest request, @RequestParam Long idSolicitante)
            throws ProductoNoEncontradoException, CategoriaNoEncontradaException,
            UsuarioNoEncontradoException, OperacionAjenaException {
        return ResponseEntity.ok(productoService.updateProducto(idProducto, request, idSolicitante));
    }

    /**
     * Pausar o reanudar la publicacion, como en los marketplaces conocidos.
     *
     * El estado llega como enum, asi que Spring rechaza con 400 cualquier valor
     * que no exista sin que haya que validarlo a mano.
     */
    @PutMapping("/{idProducto}/estado")
    public ResponseEntity<ProductoResponse> cambiarEstadoPublicacion(
            @PathVariable Long idProducto, @RequestParam EstadoPublicacion estado,
            @RequestParam Long idSolicitante)
            throws ProductoNoEncontradoException, OperacionAjenaException,
            TransicionInvalidaException {
        return ResponseEntity.ok(
                productoService.cambiarEstadoPublicacion(idProducto, estado, idSolicitante));
    }

    @DeleteMapping("/{idProducto}")
    public ResponseEntity<MensajeResponse> deleteProducto(@PathVariable Long idProducto,
            @RequestParam Long idSolicitante)
            throws ProductoNoEncontradoException, OperacionAjenaException {
        productoService.deleteProducto(idProducto, idSolicitante);
        return ResponseEntity.ok(new MensajeResponse("Producto dado de baja correctamente"));
    }
}
