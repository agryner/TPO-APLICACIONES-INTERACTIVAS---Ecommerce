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
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.exceptions.CategoriaNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.OrdenamientoInvalidoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.TransicionInvalidaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.service.ProductoService;

import lombok.RequiredArgsConstructor;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.AdminNoComerciaException;

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
            @RequestParam(required = false) EstadoPublicacion estado)
            throws UsuarioNoEncontradoException {
        return ResponseEntity.ok(productoService.getMisPublicaciones(idSolicitante, estado));
    }

    @GetMapping("/{idProducto}")
    public ResponseEntity<ProductoResponse> getProductoById(@PathVariable Long idProducto)
            throws ProductoNoEncontradoException {
        return ResponseEntity.ok(productoService.getProductoById(idProducto));
    }

    @PostMapping
    public ResponseEntity<Object> createProducto(@Valid @RequestBody ProductoRequest request,
            @RequestParam Long idSolicitante)
            throws CategoriaNoEncontradaException, UsuarioNoEncontradoException, CuentaInactivaException, AdminNoComerciaException {
        ProductoCreadoResponse result = productoService.createProducto(request, idSolicitante);
        return ResponseEntity.created(URI.create("/productos/" + result.getProducto().getId()))
                .body(result);
    }

    @PutMapping("/{idProducto}")
    public ResponseEntity<ProductoResponse> updateProducto(@PathVariable Long idProducto,
            @Valid @RequestBody ProductoRequest request, @RequestParam Long idSolicitante)
            throws ProductoNoEncontradoException, CategoriaNoEncontradaException,
            UsuarioNoEncontradoException, OperacionAjenaException, CuentaInactivaException {
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
            TransicionInvalidaException, CuentaInactivaException, UsuarioNoEncontradoException {
        return ResponseEntity.ok(
                productoService.cambiarEstadoPublicacion(idProducto, estado, idSolicitante));
    }

    /** Su vendedor o el ADMIN: devuelve al catalogo un producto dado de baja. */
    @PutMapping("/{idProducto}/reactivar")
    public ResponseEntity<ProductoResponse> reactivar(@PathVariable Long idProducto,
            @RequestParam Long idSolicitante)
            throws ProductoNoEncontradoException, OperacionAjenaException, CuentaInactivaException,
            UsuarioNoEncontradoException {
        return ResponseEntity.ok(productoService.reactivarProducto(idProducto, idSolicitante));
    }

    /**
     * Solo ADMIN: el catalogo entero, incluidos los inactivos, los borradores y
     * los pausados, que el listado publico esconde.
     */
    @GetMapping("/todos")
    public ResponseEntity<List<ProductoResponse>> getTodos(@RequestParam Long idSolicitante,
            @RequestParam(required = false) EstadoPublicacion estado)
            throws UsuarioNoEncontradoException, AccesoDenegadoException {
        return ResponseEntity.ok(productoService.getTodosLosProductos(idSolicitante, estado));
    }

    @DeleteMapping("/{idProducto}")
    public ResponseEntity<MensajeResponse> deleteProducto(@PathVariable Long idProducto,
            @RequestParam Long idSolicitante)
            throws ProductoNoEncontradoException, OperacionAjenaException, CuentaInactivaException, UsuarioNoEncontradoException {
        productoService.deleteProducto(idProducto, idSolicitante);
        return ResponseEntity.ok(new MensajeResponse("Producto dado de baja correctamente"));
    }
}
