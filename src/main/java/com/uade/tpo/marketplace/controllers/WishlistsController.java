package com.uade.tpo.marketplace.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.entity.dto.ItemWishlistRequest;
import com.uade.tpo.marketplace.entity.dto.WishlistResponse;
import com.uade.tpo.marketplace.exceptions.AdminNoComerciaException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.ItemWishlistNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.service.WishlistService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Endpoints REST de la wishlist.
 *
 * Cuelga del usuario y no tiene ruta propia, por el mismo motivo que el
 * carrito: hay exactamente una por persona, asi que no hace falta un id que la
 * identifique aparte.
 *
 * Delega en WishlistService y devuelve siempre la lista completa, para que el
 * cliente no tenga que volver a pedirla despues de cada cambio.
 */
@RestController
@RequestMapping("usuarios/{idUsuario}/wishlist")
@RequiredArgsConstructor
public class WishlistsController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<WishlistResponse> obtenerWishlist(@PathVariable Long idUsuario,
            @RequestParam Long idSolicitante)
            throws OperacionAjenaException, UsuarioNoEncontradoException, CuentaInactivaException {
        return ResponseEntity.ok(wishlistService.obtenerWishlist(idUsuario, idSolicitante));
    }

    @PostMapping("/items")
    public ResponseEntity<WishlistResponse> agregarItem(@PathVariable Long idUsuario,
            @Valid @RequestBody ItemWishlistRequest request, @RequestParam Long idSolicitante)
            throws OperacionAjenaException, UsuarioNoEncontradoException,
            ProductoNoEncontradoException, CuentaInactivaException, AdminNoComerciaException {
        return ResponseEntity.ok(wishlistService.agregarItem(idUsuario, request, idSolicitante));
    }

    @DeleteMapping("/items/{idItem}")
    public ResponseEntity<WishlistResponse> eliminarItem(@PathVariable Long idUsuario,
            @PathVariable Long idItem, @RequestParam Long idSolicitante)
            throws OperacionAjenaException, UsuarioNoEncontradoException,
            ItemWishlistNoEncontradoException, CuentaInactivaException {
        return ResponseEntity.ok(wishlistService.eliminarItem(idUsuario, idItem, idSolicitante));
    }

    @DeleteMapping("/items")
    public ResponseEntity<WishlistResponse> vaciar(@PathVariable Long idUsuario,
            @RequestParam Long idSolicitante)
            throws OperacionAjenaException, UsuarioNoEncontradoException, CuentaInactivaException {
        return ResponseEntity.ok(wishlistService.vaciar(idUsuario, idSolicitante));
    }
}
