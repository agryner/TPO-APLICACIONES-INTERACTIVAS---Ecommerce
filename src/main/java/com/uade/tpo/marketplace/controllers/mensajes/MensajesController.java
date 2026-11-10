package com.uade.tpo.marketplace.controllers.mensajes;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.ChatNoDisponibleException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.OrdenNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;
import com.uade.tpo.marketplace.service.MensajeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// El chat de cada orden, para que comprador y vendedor coordinen la entrega.
@RestController
@RequestMapping("ordenes/{idOrden}/mensajes")
@RequiredArgsConstructor
public class MensajesController {
    private final MensajeService mensajeService;

    /**
     * Pre : el id de la orden y el token de su comprador, su vendedor o un
     *       ADMIN.
     * Post: los mensajes en orden de llegada. 403 si no sos parte, 404 si no
     *       hay ninguno todavia.
     */
    @GetMapping
    public ResponseEntity<List<MensajeResponse>> getMensajes(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long idOrden)
            throws OrdenNoEncontradaException, OperacionAjenaException, SinResultadosException {
        return ResponseEntity.ok(mensajeService.getMensajes(idOrden, usuario.getId()));
    }

    /**
     * Pre : el id de la orden, el token del comprador o del vendedor y el
     *       texto (1 a 500 caracteres).
     * Post: 201 con el mensaje; la otra parte recibe una notificacion. 403 si
     *       no sos parte, 409 si la orden no esta pagada.
     */
    @PostMapping
    public ResponseEntity<MensajeResponse> enviar(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long idOrden,
            @Valid @RequestBody MensajeRequest request)
            throws OrdenNoEncontradaException, OperacionAjenaException, ChatNoDisponibleException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mensajeService.enviar(idOrden, usuario.getId(), request.getTexto()));
    }
}
