package com.uade.tpo.marketplace.controllers.carritos;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.AdminNoComerciaException;
import com.uade.tpo.marketplace.exceptions.CantidadInvalidaException;
import com.uade.tpo.marketplace.exceptions.CompraPropiaException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.ItemCarritoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.StockInsuficienteException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.service.CarritoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("carrito")
@RequiredArgsConstructor
public class CarritosController {
    private final CarritoService carritoService;

    /**
     * Pre : solo el token.
     * Post: el carrito con sus items, el subtotal y el total. Se crea vacio si
     *       es la primera vez, y se vacia solo si pasaron los 30 dias sin que
     *       nadie lo tocara.
     */
    @GetMapping
    public ResponseEntity<CarritoResponse> obtenerCarrito(@AuthenticationPrincipal Usuario usuario)
            throws UsuarioNoEncontradoException, CuentaInactivaException {
        return ResponseEntity.ok(carritoService.obtenerCarrito(usuario.getId()));
    }

    /**
     * Pre : el body con idProducto y cantidad, y el token. La cantidad tiene
     *       que ser al menos 1 y, sumada a lo que ya hubiera, no superar el
     *       stock.
     * Post: el carrito con los totales recalculados y la vigencia corrida 30
     *       dias. El stock NO se descuenta: eso pasa recien en el checkout.
     *       404 si el producto no esta publicado, 400 si falta stock, 409 si el
     *       producto es tuyo, 403 si quien pide es ADMIN.
     */
    @PostMapping("/items")
    public ResponseEntity<CarritoResponse> agregarItem(@Valid @RequestBody ItemCarritoRequest request,
            @AuthenticationPrincipal Usuario usuario)
            throws UsuarioNoEncontradoException, ProductoNoEncontradoException,
            StockInsuficienteException, CuentaInactivaException, CompraPropiaException,
            CantidadInvalidaException, AdminNoComerciaException {
        return ResponseEntity.ok(carritoService.agregarItem(usuario.getId(), request));
    }

    /**
     * Pre : el id del item en la ruta, el body con la cantidad nueva, y el
     *       token. El id del item sale de la respuesta de agregar, no es el id
     *       del producto.
     * Post: el carrito con la cantidad puesta en el valor nuevo y los totales
     *       recalculados. Con cantidad cero o negativa el item se elimina, que
     *       es la unica diferencia con agregar. 400 si la cantidad supera el
     *       stock, 404 si ese item no esta en tu carrito.
     */
    @PutMapping("/items/{idItem}")
    public ResponseEntity<CarritoResponse> modificarCantidad(@PathVariable Long idItem,
            @RequestBody ItemCarritoRequest request, @AuthenticationPrincipal Usuario usuario)
            throws UsuarioNoEncontradoException, ItemCarritoNoEncontradoException,
            StockInsuficienteException, CuentaInactivaException {
        return ResponseEntity.ok(carritoService.modificarCantidad(
                usuario.getId(), idItem, request.getCantidad()));
    }

    /**
     * Pre : el id del item en la ruta y el token.
     * Post: el carrito sin ese item y con los totales recalculados. 404 si ese
     *       item no esta en tu carrito.
     */
    @DeleteMapping("/items/{idItem}")
    public ResponseEntity<CarritoResponse> eliminarItem(@PathVariable Long idItem,
            @AuthenticationPrincipal Usuario usuario)
            throws UsuarioNoEncontradoException, ItemCarritoNoEncontradoException,
            CuentaInactivaException {
        return ResponseEntity.ok(carritoService.eliminarItem(usuario.getId(), idItem));
    }

    /**
     * Pre : solo el token.
     * Post: el carrito vacio, con los totales en cero. El carrito en si no se
     *       borra, y al quedar sin items deja de vencer.
     */
    @DeleteMapping
    public ResponseEntity<CarritoResponse> vaciar(@AuthenticationPrincipal Usuario usuario)
            throws UsuarioNoEncontradoException, CuentaInactivaException {
        return ResponseEntity.ok(carritoService.vaciar(usuario.getId()));
    }
}
