package com.uade.tpo.marketplace.controllers.resenas;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.EntregaPendienteException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.OrdenNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.ProductoFueraDeLaOrdenException;
import com.uade.tpo.marketplace.exceptions.ResenaDuplicadaException;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.service.ResenaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("resenas")
@RequiredArgsConstructor
public class ResenasController {
    private final ResenaService resenaService;

    /**
     * Pre : el body con idOrden, idProducto, puntaje de 1 a 5 y un comentario
     *       opcional, mas el token del comprador.
     * Post: 201 con la resena. 403 si la orden no es tuya, 400 si el producto
     *       no esta en esa orden, 409 si el envio todavia no figura entregado
     *       o si ya calificaste ese producto en esa orden.
     */
    @PostMapping
    public ResponseEntity<ResenaResponse> crear(@Valid @RequestBody ResenaRequest request,
            @AuthenticationPrincipal Usuario usuario)
            throws OrdenNoEncontradaException, OperacionAjenaException,
            ProductoFueraDeLaOrdenException, EntregaPendienteException,
            ResenaDuplicadaException, UsuarioNoEncontradoException {
        ResenaResponse creada = resenaService.crear(request, usuario.getId());
        return ResponseEntity.created(URI.create("/resenas/" + creada.getId())).body(creada);
    }

    /**
     * Pre : el id del producto en la ruta. Es publico.
     * Post: sus resenas, las mas nuevas primero. 404 si no tiene ninguna.
     */
    @GetMapping("/producto/{idProducto}")
    public ResponseEntity<List<ResenaResponse>> getDeProducto(@PathVariable Long idProducto)
            throws SinResultadosException {
        return ResponseEntity.ok(resenaService.getDeProducto(idProducto));
    }

    /**
     * Pre : el nombre de usuario del vendedor en la ruta. Es publico.
     * Post: el promedio de las resenas de todos sus productos y cuantas son.
     *       Con promedio en null si todavia no tiene ninguna, que no es lo
     *       mismo que un promedio de cero.
     */
    @GetMapping("/vendedor/{nombreUsuario}")
    public ResponseEntity<CalificacionResponse> getCalificacion(
            @PathVariable String nombreUsuario) throws UsuarioNoEncontradoException {
        return ResponseEntity.ok(resenaService.getCalificacion(nombreUsuario));
    }
}
