package com.uade.tpo.marketplace.controllers.wishlist;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.AdminNoComerciaException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.ItemWishlistNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.service.WishlistService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * La lista de deseos del usuario logueado.
 *
 * Misma forma que el carrito: la ruta no lleva id de persona, porque hay una
 * wishlist por usuario y quien pide sale del token. El unico @PathVariable es
 * el id del item.
 *
 * Delega en WishlistService y devuelve siempre la lista completa, para que el
 * cliente no tenga que volver a pedirla despues de cada cambio.
 */
@RestController
@RequestMapping("wishlist")
@RequiredArgsConstructor
public class WishlistsController {

    private final WishlistService wishlistService;

    /**
     * Lo que guarde para mas adelante.
     *
     * Pre : solo el token.
     * Post: la wishlist con sus items, cada uno con el producto y si hoy esta
     *       disponible. Se crea vacia si es la primera vez, y se vacia sola si
     *       pasaron los 8 meses.
     */
    @GetMapping
    public ResponseEntity<WishlistResponse> obtenerWishlist(@AuthenticationPrincipal Usuario usuario)
            throws UsuarioNoEncontradoException, CuentaInactivaException {
        return ResponseEntity.ok(wishlistService.obtenerWishlist(usuario.getId()));
    }

    /**
     * Guarda un producto para mas adelante.
     *
     * Pre : el body con idProducto y el token. El producto tiene que estar a la
     *       venta en este momento.
     * Post: la wishlist completa con el producto adentro. Si ya estaba no hace
     *       nada, asi que se puede repetir sin duplicar. 404 si el producto no
     *       existe o no esta publicado, 403 si quien pide es ADMIN.
     */
    @PostMapping("/items")
    public ResponseEntity<WishlistResponse> agregarItem(
            @Valid @RequestBody ItemWishlistRequest request,
            @AuthenticationPrincipal Usuario usuario)
            throws UsuarioNoEncontradoException, ProductoNoEncontradoException,
            CuentaInactivaException, AdminNoComerciaException {
        return ResponseEntity.ok(wishlistService.agregarItem(usuario.getId(), request));
    }

    /**
     * Saca un producto de la lista.
     *
     * Pre : el id del item en la ruta y el token. El id del item sale de la
     *       respuesta de agregar, no es el del producto.
     * Post: la wishlist sin ese item. 404 si ese item no esta en tu lista.
     */
    @DeleteMapping("/items/{idItem}")
    public ResponseEntity<WishlistResponse> eliminarItem(@PathVariable Long idItem,
            @AuthenticationPrincipal Usuario usuario)
            throws UsuarioNoEncontradoException, ItemWishlistNoEncontradoException,
            CuentaInactivaException {
        return ResponseEntity.ok(wishlistService.eliminarItem(usuario.getId(), idItem));
    }

    /**
     * Saca todo de una.
     *
     * Pre : solo el token.
     * Post: la wishlist vacia. La lista en si no se borra, y al quedar sin
     *       items deja de vencer.
     */
    @DeleteMapping
    public ResponseEntity<WishlistResponse> vaciar(@AuthenticationPrincipal Usuario usuario)
            throws UsuarioNoEncontradoException, CuentaInactivaException {
        return ResponseEntity.ok(wishlistService.vaciar(usuario.getId()));
    }
}
