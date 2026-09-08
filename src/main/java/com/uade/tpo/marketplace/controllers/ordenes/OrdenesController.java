package com.uade.tpo.marketplace.controllers.ordenes;

import com.uade.tpo.marketplace.entity.EstadoOrden;
import com.uade.tpo.marketplace.controllers.ordenes.OrdenDeCompraResponse;
import com.uade.tpo.marketplace.controllers.ordenes.RolEnOrden;
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
import com.uade.tpo.marketplace.exceptions.CompraPropiaException;
import com.uade.tpo.marketplace.exceptions.OrdenNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.StockInsuficienteException;
import com.uade.tpo.marketplace.exceptions.TransicionInvalidaException;
import com.uade.tpo.marketplace.service.OrdenDeCompraService;

import lombok.RequiredArgsConstructor;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.AdminNoComerciaException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.uade.tpo.marketplace.entity.Usuario;

@RestController
@RequestMapping("ordenes")
@RequiredArgsConstructor
public class OrdenesController {
    private final OrdenDeCompraService ordenService;

    /**
     * Pre : el token, y opcionalmente rol para mirar una sola punta.
     * Post: las ordenes donde el usuario participa. Sin rol, las compras y las
     *       ventas juntas; un ADMIN sin rol recibe todas las del sistema. 400
     *       si el rol no es COMPRADOR ni VENDEDOR.
     */
    @GetMapping
    public ResponseEntity<List<OrdenDeCompraResponse>> getOrdenes(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam(required = false) RolEnOrden rol)
            throws UsuarioNoEncontradoException {
        return ResponseEntity.ok(ordenService.getOrdenes(usuario.getId(), rol));
    }

    /**
     * Pre : el id en la ruta y el token.
     * Post: la orden con comprador, vendedor, estado y renglones. 403 si no
     *       sos parte de esa orden ni ADMIN, 404 si no existe.
     */
    @GetMapping("/{idOrden}")
    public ResponseEntity<OrdenDeCompraResponse> getOrdenById(@PathVariable Long idOrden,
            @AuthenticationPrincipal Usuario usuario)
            throws OrdenNoEncontradaException, OperacionAjenaException {
        return ResponseEntity.ok(ordenService.getOrdenById(idOrden, usuario.getId()));
    }

    /**
     * Pre : solo el token: el contenido sale del carrito de quien pide.
     * Post: 201 con una orden por cada vendedor involucrado, y el carrito
     *       vacio. Descuenta el stock. Valida todo antes de escribir, asi que
     *       si un item falla no queda ninguna orden a medias. 400 si el
     *       carrito esta vacio o falta stock, 403 si quien pide es ADMIN.
     */
    @PostMapping
    public ResponseEntity<List<OrdenDeCompraResponse>> createOrden(
            @AuthenticationPrincipal Usuario usuario)
            throws UsuarioNoEncontradoException, CarritoVacioException, StockInsuficienteException,
            ProductoNoEncontradoException, CompraPropiaException, CuentaInactivaException, AdminNoComerciaException {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordenService.createOrden(usuario.getId()));
    }

    /**
     * Pre : el id en la ruta, el estado destino como enum, y el token.
     * Post: la orden en el estado nuevo. PAGADA solo la marca el ADMIN, porque
     *       sin pasarela ninguna de las partes puede probar que el dinero
     *       entro. CANCELADA la piden comprador o vendedor, pero solo sobre una
     *       orden PENDIENTE: una vez pagada la orden esta cerrada y no se
     *       cancela ni para el ADMIN. Cancelar repone el stock. 403 si no te
     *       toca ese paso, 409 si el salto no existe desde el estado actual,
     *       400 si el estado no existe.
     */
    @PutMapping("/{idOrden}/estado")
    public ResponseEntity<OrdenDeCompraResponse> actualizarEstado(@PathVariable Long idOrden,
            @RequestParam EstadoOrden estado, @AuthenticationPrincipal Usuario usuario)
            throws OrdenNoEncontradaException, TransicionInvalidaException,
            CambioDeEstadoNoPermitidoException {
        return ResponseEntity.ok(ordenService.actualizarEstado(idOrden, estado, usuario.getId()));
    }
}
