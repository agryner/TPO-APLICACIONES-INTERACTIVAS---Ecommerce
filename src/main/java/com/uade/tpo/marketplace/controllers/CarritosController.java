package com.uade.tpo.marketplace.controllers;

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

import com.uade.tpo.marketplace.entity.dto.CarritoResponse;
import com.uade.tpo.marketplace.entity.dto.ItemCarritoRequest;
import com.uade.tpo.marketplace.exceptions.ItemCarritoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.CompraPropiaException;
import com.uade.tpo.marketplace.exceptions.CantidadInvalidaException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.StockInsuficienteException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.service.CarritoService;

import lombok.RequiredArgsConstructor;
import com.uade.tpo.marketplace.exceptions.AdminNoComerciaException;

/**
 * El carrito es unico por usuario, asi que cuelga del usuario y no tiene id
 * propio en la URL.
 *
 * Todas las operaciones son del duenio: el idUsuario de la ruta dice de quien
 * es el carrito y el idSolicitante quien pide la operacion, y el service exige
 * que coincidan. Cuando se sume la autenticacion, el idSolicitante sale del
 * token y el parametro desaparece.
 *
 * Delega todo en CarritoService y devuelve un CarritoResponse ya con sus
 * totales recalculados.
 */
@RestController
@RequestMapping("usuarios/{idUsuario}/carrito")
@RequiredArgsConstructor
public class CarritosController {

    private final CarritoService carritoService;

    @GetMapping
    public ResponseEntity<CarritoResponse> obtenerCarrito(@PathVariable Long idUsuario,
            @RequestParam Long idSolicitante)
            throws OperacionAjenaException, UsuarioNoEncontradoException, CuentaInactivaException {
        return ResponseEntity.ok(carritoService.obtenerCarrito(idUsuario, idSolicitante));
    }

    @PostMapping("/items")
    public ResponseEntity<CarritoResponse> agregarItem(@PathVariable Long idUsuario,
            @Valid @RequestBody ItemCarritoRequest request, @RequestParam Long idSolicitante)
            throws OperacionAjenaException, UsuarioNoEncontradoException,
            ProductoNoEncontradoException, StockInsuficienteException,
            CompraPropiaException, CantidadInvalidaException, CuentaInactivaException, AdminNoComerciaException {
        return ResponseEntity.ok(carritoService.agregarItem(idUsuario, request, idSolicitante));
    }

    @PutMapping("/items/{idItem}")
    public ResponseEntity<CarritoResponse> modificarCantidad(@PathVariable Long idUsuario,
            // Sin @Valid a proposito: este endpoint manda solo la cantidad, y el
            // @NotNull de idProducto lo rechazaria. La cantidad la valida el
            // service, que es quien sabe que aca un cero significa "sacalo".
            @PathVariable Long idItem, @RequestBody ItemCarritoRequest request,
            @RequestParam Long idSolicitante)
            throws OperacionAjenaException, UsuarioNoEncontradoException,
            ItemCarritoNoEncontradoException, StockInsuficienteException, CuentaInactivaException {
        return ResponseEntity.ok(carritoService.modificarCantidad(
                idUsuario, idItem, request.getCantidad(), idSolicitante));
    }

    @DeleteMapping("/items/{idItem}")
    public ResponseEntity<CarritoResponse> eliminarItem(@PathVariable Long idUsuario,
            @PathVariable Long idItem, @RequestParam Long idSolicitante)
            throws OperacionAjenaException, UsuarioNoEncontradoException,
            ItemCarritoNoEncontradoException, CuentaInactivaException {
        return ResponseEntity.ok(carritoService.eliminarItem(idUsuario, idItem, idSolicitante));
    }

    @DeleteMapping("/items")
    public ResponseEntity<CarritoResponse> vaciar(@PathVariable Long idUsuario,
            @RequestParam Long idSolicitante)
            throws OperacionAjenaException, UsuarioNoEncontradoException, CuentaInactivaException {
        return ResponseEntity.ok(carritoService.vaciar(idUsuario, idSolicitante));
    }
}
