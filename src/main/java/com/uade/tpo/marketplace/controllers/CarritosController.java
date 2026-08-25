package com.uade.tpo.marketplace.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.entity.Carrito;
import com.uade.tpo.marketplace.entity.dto.ItemCarritoRequest;
import com.uade.tpo.marketplace.exceptions.ItemCarritoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.StockInsuficienteException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.service.CarritoService;

import lombok.RequiredArgsConstructor;

/**
 * El carrito es unico por usuario, asi que cuelga del usuario y no tiene id propio
 * en la URL. Cuando se sume la autenticacion, el {idUsuario} sale del token.
 */
@RestController
@RequestMapping("usuarios/{idUsuario}/carrito")
@RequiredArgsConstructor
public class CarritosController {

    private final CarritoService carritoService;

    @GetMapping
    public ResponseEntity<Carrito> obtenerCarrito(@PathVariable Long idUsuario)
            throws UsuarioNoEncontradoException {
        return ResponseEntity.ok(carritoService.obtenerCarrito(idUsuario));
    }

    @PostMapping("/items")
    public ResponseEntity<Carrito> agregarItem(@PathVariable Long idUsuario,
            @RequestBody ItemCarritoRequest request)
            throws UsuarioNoEncontradoException, ProductoNoEncontradoException, StockInsuficienteException {
        return ResponseEntity.ok(carritoService.agregarItem(idUsuario, request));
    }

    @DeleteMapping("/items/{idItem}")
    public ResponseEntity<Carrito> eliminarItem(@PathVariable Long idUsuario, @PathVariable Long idItem)
            throws UsuarioNoEncontradoException, ItemCarritoNoEncontradoException {
        return ResponseEntity.ok(carritoService.eliminarItem(idUsuario, idItem));
    }

    @DeleteMapping("/items")
    public ResponseEntity<Carrito> vaciar(@PathVariable Long idUsuario) throws UsuarioNoEncontradoException {
        return ResponseEntity.ok(carritoService.vaciar(idUsuario));
    }
}
