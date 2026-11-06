package com.uade.tpo.marketplace.controllers.notificaciones;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.controllers.common.PaginaResponse;
import com.uade.tpo.marketplace.entity.TipoNotificacion;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.entity.OrdenDeCompra; // <-- Importación agregada
import com.uade.tpo.marketplace.exceptions.NotificacionNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;
import com.uade.tpo.marketplace.service.NotificacionService;
import com.uade.tpo.marketplace.repository.OrdenDeCompraRepository; // <-- Importación agregada
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("notificaciones")
@RequiredArgsConstructor
public class NotificacionesController {
    
    private final NotificacionService notificacionService;
    private final OrdenDeCompraRepository ordenDeCompraRepository; // <-- Repositorio inyectado

    /**
     * Pre : solo el token. No recibe ningun id: son siempre las propias.
     * Post: las notificaciones del usuario, las mas nuevas primero. 404 si no
     *       tiene ninguna.
     */
    @GetMapping
    public ResponseEntity<PaginaResponse<NotificacionResponse>> getMias(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanio) throws SinResultadosException {
        return ResponseEntity.ok(PaginaResponse.de(
                notificacionService.getMias(usuario.getId()), pagina, tamanio));
    }

    /**
     * Pre : solo el token.
     * Post: cuantas tiene sin leer. Es para el badge de la campanita, por eso
     *       devuelve un numero y no la lista: el front lo puede pedir seguido
     *       sin bajarse todo.
     */
    @GetMapping("/no-leidas")
    public ResponseEntity<NoLeidasResponse> contarNoLeidas(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(new NoLeidasResponse(
                notificacionService.contarNoLeidas(usuario.getId())));
    }

    /**
     * Pre : el id en la ruta y el token de su destinatario.
     * Post: la notificacion marcada como leida. 403 si es de otro, 404 si no
     *       existe.
     */
    @PutMapping("/{idNotificacion}/leida")
    public ResponseEntity<NotificacionResponse> marcarLeida(
            @PathVariable Long idNotificacion, @AuthenticationPrincipal Usuario usuario)
            throws NotificacionNoEncontradaException, OperacionAjenaException {
        return ResponseEntity.ok(
                notificacionService.marcarLeida(idNotificacion, usuario.getId()));
    }

   @PostMapping("/chat/notificar")
    @Transactional
    public ResponseEntity<Void> notificarMensajeChat(
            @RequestParam Long idOrden,
            @AuthenticationPrincipal Usuario usuario) {
        
        OrdenDeCompra orden = ordenDeCompraRepository.findById(idOrden).orElse(null);
        if (orden == null) {
            return ResponseEntity.ok().build();
        }

        Usuario comprador = orden.getComprador();
        Usuario vendedor = orden.getVendedor();
        
        boolean esComprador = usuario.getId().equals(comprador.getId());
        Usuario destinatario = esComprador ? vendedor : comprador;

        String texto = "Tienes un mensaje en el chat de la orden #" + orden.getId();
        String link = "/ordenes/" + orden.getId();
        
        notificacionService.crear(destinatario, TipoNotificacion.ORDEN_ACTUALIZADA, texto, link);
        
        return ResponseEntity.ok().build();
    }
}