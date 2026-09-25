package com.uade.tpo.marketplace.controllers.envios;

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

import com.uade.tpo.marketplace.entity.EstadoEnvio;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.CambioDeEstadoNoPermitidoException;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.EnvioNoDisponibleException;
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
     *       DESPACHANTE, los que tiene en la mano: los que el mismo cargo por
     *       numero y todavia no entrego. Para el ADMIN, todos. 404 si no hay
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
     *       comprador, su vendedor y el ADMIN. Un DESPACHANTE solo los de su
     *       cola, los DESPACHADO y EN_TRANSITO: por id podria recorrer todos y
     *       quedarse con las direcciones de entrega de todo el mundo. 403 para
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
     * Pre : solo un token de DESPACHANTE.
     * Post: lo que entrego, lo mas nuevo primero: el numero y la fecha, nada
     *       mas. Sin comprador ni direccion, porque el historial cuenta lo que
     *       hizo el y no por quienes paso; si guardara las direcciones, quien
     *       entrego mil paquetes se quedaria con mil direcciones. 403 si no sos
     *       DESPACHANTE, 404 si todavia no entregaste nada.
     */
    @GetMapping("/historial")
    public ResponseEntity<List<EntregaResponse>> getHistorial(
            @AuthenticationPrincipal Usuario usuario)
            throws UsuarioNoEncontradoException, AccesoDenegadoException,
            SinResultadosException {
        return ResponseEntity.ok(envioService.getHistorial(usuario.getId()));
    }

    /**
     * Pre : el numero que figura en la etiqueta del bulto y un token de
     *       DESPACHANTE.
     * Post: el envio, ya suyo y EN_TRANSITO. Cargar el numero ES tomar el
     *       envio: en una sucursal uno carga el paquete que tiene en la mano,
     *       no uno de una lista, y por eso este es el momento en que aparece la
     *       direccion de entrega. 403 si no sos DESPACHANTE, 404 si ese numero
     *       no existe, 409 si ya lo tomo otro o si el vendedor todavia no lo
     *       despacho.
     */
    @PostMapping("/recibir")
    public ResponseEntity<EnvioResponse> recibir(@RequestParam String numero,
            @AuthenticationPrincipal Usuario usuario)
            throws EnvioNoEncontradoException, EnvioNoDisponibleException,
            CambioDeEstadoNoPermitidoException, UsuarioNoEncontradoException {
        return ResponseEntity.ok(envioService.recibir(numero, usuario.getId()));
    }

    /**
     * Pre : el id en la ruta, el estado destino como enum, y el token.
     * Post: el envio en el estado nuevo. DESPACHADO lo pide el vendedor, y ahi
     *       se genera el numero de seguimiento. ENTREGADO lo pide el
     *       despachante que lo cargo, o el comprador si la entrega se coordino.
     *       EN_TRANSITO no se pide por aca: se llega cargando el numero en
     *       POST /envios/recibir. El camino no tiene vuelta atras. 403 si no te
     *       toca ese paso, 409 si el salto no existe desde el estado actual,
     *       400 si el estado no existe.
     */
    @PutMapping("/{idEnvio}/estado")
    public ResponseEntity<EnvioResponse> actualizarEstado(@PathVariable Long idEnvio,
            @RequestParam EstadoEnvio estado, @AuthenticationPrincipal Usuario usuario)
            throws EnvioNoEncontradoException, TransicionInvalidaException,
            CambioDeEstadoNoPermitidoException, UsuarioNoEncontradoException {
        return ResponseEntity.ok(envioService.actualizarEstado(idEnvio, estado, usuario.getId()));
    }
}
