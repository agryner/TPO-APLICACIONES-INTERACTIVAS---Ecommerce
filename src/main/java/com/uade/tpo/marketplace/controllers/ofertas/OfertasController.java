package com.uade.tpo.marketplace.controllers.ofertas;

import java.net.URI;
import java.util.List;

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
import com.uade.tpo.marketplace.exceptions.CompraPropiaException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.OfertaDuplicadaException;
import com.uade.tpo.marketplace.exceptions.OfertaNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.OfertaYaRespondidaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.PrecioOfrecidoInvalidoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoAceptaOfertasException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.RolNoComerciaException;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;
import com.uade.tpo.marketplace.exceptions.StockInsuficienteException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.service.OfertaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("ofertas")
@RequiredArgsConstructor
public class OfertasController {
    private final OfertaService ofertaService;

    /**
     * Pre : el body con idProducto, cantidad y precioOfrecido por unidad, mas
     *       el token del comprador.
     * Post: 201 con la oferta pendiente, y el vendedor recibe una notificacion.
     *       409 si es tu propio producto, si ya tenes una pendiente por el
     *       mismo, o si el vendedor no puso ese producto a negociar. 400 si
     *       ofreces el precio de lista o mas, 403 si tu rol no comercia.
     */
    @PostMapping
    public ResponseEntity<OfertaResponse> crear(@Valid @RequestBody OfertaRequest request,
            @AuthenticationPrincipal Usuario usuario)
            throws ProductoNoEncontradoException, CompraPropiaException,
            PrecioOfrecidoInvalidoException, OfertaDuplicadaException,
            ProductoNoAceptaOfertasException, UsuarioNoEncontradoException,
            CuentaInactivaException, RolNoComerciaException {
        OfertaResponse creada = ofertaService.crear(request, usuario.getId());
        return ResponseEntity.created(URI.create("/ofertas/" + creada.getId())).body(creada);
    }

    /**
     * Pre : solo el token. No recibe ningun id.
     * Post: las ofertas que hiciste y las que recibiste. 404 si no tenes
     *       ninguna.
     */
    @GetMapping
    public ResponseEntity<List<OfertaResponse>> getMias(@AuthenticationPrincipal Usuario usuario)
            throws SinResultadosException {
        return ResponseEntity.ok(ofertaService.getMias(usuario.getId()));
    }

    /**
     * Pre : el id en la ruta y el token del vendedor del producto.
     * Post: la oferta aceptada y la orden ya creada al precio acordado: si
     *       aceptaste, la venta esta cerrada. 403 si el producto no es tuyo,
     *       409 si ya fue respondida, 400 si mientras tanto te quedaste sin
     *       stock.
     */
    @PutMapping("/{idOferta}/aceptar")
    public ResponseEntity<OfertaResponse> aceptar(@PathVariable Long idOferta,
            @AuthenticationPrincipal Usuario usuario)
            throws OfertaNoEncontradaException, OperacionAjenaException,
            OfertaYaRespondidaException, StockInsuficienteException {
        return ResponseEntity.ok(ofertaService.aceptar(idOferta, usuario.getId()));
    }

    /**
     * Pre : el id en la ruta y el token del vendedor del producto.
     * Post: la oferta rechazada. El comprador puede volver a ofertar mas alto.
     */
    @PutMapping("/{idOferta}/rechazar")
    public ResponseEntity<OfertaResponse> rechazar(@PathVariable Long idOferta,
            @AuthenticationPrincipal Usuario usuario)
            throws OfertaNoEncontradaException, OperacionAjenaException,
            OfertaYaRespondidaException {
        return ResponseEntity.ok(ofertaService.rechazar(idOferta, usuario.getId()));
    }

    /**
     * Pre : el id en la ruta y el token de quien la hizo.
     * Post: la oferta cancelada. Solo la retira su autor y solo mientras siga
     *       pendiente.
     */
    @DeleteMapping("/{idOferta}")
    public ResponseEntity<OfertaResponse> cancelar(@PathVariable Long idOferta,
            @AuthenticationPrincipal Usuario usuario)
            throws OfertaNoEncontradaException, OperacionAjenaException,
            OfertaYaRespondidaException {
        return ResponseEntity.ok(ofertaService.cancelar(idOferta, usuario.getId()));
    }
}
