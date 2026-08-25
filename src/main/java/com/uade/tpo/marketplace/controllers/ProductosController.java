package com.uade.tpo.marketplace.controllers;

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

import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.entity.dto.ProductoRequest;
import com.uade.tpo.marketplace.entity.dto.MensajeResponse;
import com.uade.tpo.marketplace.exceptions.CategoriaNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.service.ProductoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("productos")
@RequiredArgsConstructor
public class ProductosController {

    private final ProductoService productoService;

    @GetMapping
    public ResponseEntity<List<Producto>> getProductos(
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(required = false) Long idVendedor,
            @RequestParam(required = false) String nombre) {
        return ResponseEntity.ok(productoService.getProductos(idCategoria, idVendedor, nombre));
    }

    @GetMapping("/{idProducto}")
    public ResponseEntity<Producto> getProductoById(@PathVariable Long idProducto)
            throws ProductoNoEncontradoException {
        return ResponseEntity.ok(productoService.getProductoById(idProducto)
                .orElseThrow(ProductoNoEncontradoException::new));
    }

    @PostMapping
    public ResponseEntity<Object> createProducto(@RequestBody ProductoRequest request)
            throws CategoriaNoEncontradaException, UsuarioNoEncontradoException {
        Producto result = productoService.createProducto(request);
        return ResponseEntity.created(URI.create("/productos/" + result.getId())).body(result);
    }

    @PutMapping("/{idProducto}")
    public ResponseEntity<Producto> updateProducto(@PathVariable Long idProducto,
            @RequestBody ProductoRequest request)
            throws ProductoNoEncontradoException, CategoriaNoEncontradaException, UsuarioNoEncontradoException {
        return ResponseEntity.ok(productoService.updateProducto(idProducto, request));
    }

    @DeleteMapping("/{idProducto}")
    public ResponseEntity<MensajeResponse> deleteProducto(@PathVariable Long idProducto)
            throws ProductoNoEncontradoException {
        productoService.deleteProducto(idProducto);
        return ResponseEntity.ok(new MensajeResponse("Producto eliminado correctamente"));
    }
}
