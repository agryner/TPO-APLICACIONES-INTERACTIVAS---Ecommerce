package com.uade.tpo.marketplace.controllers.envios;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.entity.EstadoEnvio;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.CambioDeEstadoNoPermitidoException;
import com.uade.tpo.marketplace.exceptions.EnvioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;
import com.uade.tpo.marketplace.exceptions.TransicionInvalidaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.service.EnvioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("envios")
@RequiredArgsConstructor
public class EnviosController {
    private final EnvioService envioService;

    /**
     * Pre : solo el token. No recibe ningun id: lo que devuelve depende del
     *       rol de quien pregunta.
     * Post: para un CLIENTE, los envios donde compro o vendio. Para un
     *       DESPACHANTE, la cola de trabajo compartida: los despachados y los
     *       que estan en transito. Para el ADMIN, todos. 404 si no hay
     *       ninguno.
     */
    @GetMapping
    public ResponseEntity<List<EnvioResponse>> getMios(@AuthenticationPrincipal Usuario usuario)
            throws UsuarioNoEncontradoException, SinResultadosException {
        return ResponseEntity.ok(envioService.getMios(usuario.getId()));
    }

    /**
     * Pre : el id en la ruta y el token.
     * Post: el envio con su numero de seguimiento y sus fechas. Lo ven su
     *       comprador, su vendedor, cualquier DESPACHANTE y el ADMIN. 403 para
     *       cualquier otro, 404 si no existe.
     */
    @GetMapping("/{idEnvio}")
    public ResponseEntity<EnvioResponse> getById(@PathVariable Long idEnvio,
            @AuthenticationPrincipal Usuario usuario)
            throws EnvioNoEncontradoException, OperacionAjenaException,
            UsuarioNoEncontradoException {
        return ResponseEntity.ok(envioService.getById(idEnvio, usuario.getId()));
    }

    /**
     * Pre : el id en la ruta, el estado destino como enum, y el token.
     * Post: el envio en el estado nuevo. DESPACHADO lo pide el vendedor;
     *       EN_TRANSITO y ENTREGADO, un DESPACHANTE. El camino no tiene vuelta
     *       atras. 403 si no te toca ese paso, 409 si el salto no existe desde
     *       el estado actual, 400 si el estado no existe.
     */
    @PutMapping("/{idEnvio}/estado")
    public ResponseEntity<EnvioResponse> actualizarEstado(@PathVariable Long idEnvio,
            @RequestParam EstadoEnvio estado, @AuthenticationPrincipal Usuario usuario)
            throws EnvioNoEncontradoException, TransicionInvalidaException,
            CambioDeEstadoNoPermitidoException, UsuarioNoEncontradoException {
        return ResponseEntity.ok(envioService.actualizarEstado(idEnvio, estado, usuario.getId()));
    }
}
