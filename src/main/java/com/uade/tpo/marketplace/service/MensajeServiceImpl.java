package com.uade.tpo.marketplace.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.marketplace.controllers.mensajes.MensajeResponse;
import com.uade.tpo.marketplace.entity.EstadoOrden;
import com.uade.tpo.marketplace.entity.Mensaje;
import com.uade.tpo.marketplace.entity.OrdenDeCompra;
import com.uade.tpo.marketplace.entity.TipoNotificacion;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.ChatNoDisponibleException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.OrdenNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;
import com.uade.tpo.marketplace.repository.MensajeRepository;
import com.uade.tpo.marketplace.repository.OrdenDeCompraRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MensajeServiceImpl implements MensajeService {
    private final MensajeRepository mensajeRepository;
    private final OrdenDeCompraRepository ordenRepository;
    private final NotificacionService notificacionService;
    private final AutorizacionService autorizacion;

    /**
     * Pre : el id de la orden y el de quien pide.
     * Post: el chat de la orden, del mas viejo al mas nuevo. Lo leen el
     *       comprador, el vendedor y el admin (por si hay un reclamo). 403 si
     *       no sos parte, 404 si todavia nadie escribio.
     */
    public List<MensajeResponse> getMensajes(Long idOrden, Long idSolicitante)
            throws OrdenNoEncontradaException, OperacionAjenaException, SinResultadosException {
        OrdenDeCompra orden = ordenRepository.findById(idOrden)
                .orElseThrow(OrdenNoEncontradaException::new);
        if (!esParte(orden, idSolicitante) && !autorizacion.esAdmin(idSolicitante))
            throw new OperacionAjenaException();

        List<MensajeResponse> mensajes = mensajeRepository.findByOrdenIdOrderByFechaAsc(idOrden).stream()
                .map(MensajeResponse::from)
                .toList();

        if (mensajes.isEmpty())
            throw new SinResultadosException("Todavia no hay mensajes en esta orden");

        return mensajes;
    }

    /**
     * Pre : el id de la orden, el de quien escribe y el texto ya validado.
     * Post: el mensaje guardado y la otra parte avisada. Solo escriben el
     *       comprador y el vendedor, y solo con la orden PAGADA: antes no hay
     *       nada que coordinar. 403 si no sos parte, 409 si no esta pagada.
     */
    @Transactional
    public MensajeResponse enviar(Long idOrden, Long idSolicitante, String texto)
            throws OrdenNoEncontradaException, OperacionAjenaException, ChatNoDisponibleException {
        OrdenDeCompra orden = ordenRepository.findById(idOrden)
                .orElseThrow(OrdenNoEncontradaException::new);
        if (!esParte(orden, idSolicitante))
            throw new OperacionAjenaException();
        if (orden.getEstado() != EstadoOrden.PAGADA)
            throw new ChatNoDisponibleException();

        boolean escribeElComprador = orden.getComprador().getId().equals(idSolicitante);
        Usuario autor = escribeElComprador ? orden.getComprador() : orden.getVendedor();
        Usuario destinatario = escribeElComprador ? orden.getVendedor() : orden.getComprador();

        Mensaje mensaje = new Mensaje();
        mensaje.setOrden(orden);
        mensaje.setAutor(autor);
        mensaje.setTexto(texto.trim());
        mensaje.setFecha(LocalDateTime.now());
        mensaje = mensajeRepository.save(mensaje);

        notificacionService.crear(destinatario, TipoNotificacion.MENSAJE,
                recortar(autor.getNombreUsuario() + " te escribio por la orden #" + orden.getId() + ": "
                        + mensaje.getTexto()),
                "/ordenes/" + orden.getId());

        return MensajeResponse.from(mensaje);
    }

    private boolean esParte(OrdenDeCompra orden, Long idUsuario) {
        return orden.getComprador().getId().equals(idUsuario)
                || orden.getVendedor().getId().equals(idUsuario);
    }

    // La notificacion entra en 300 caracteres.
    private String recortar(String texto) {
        return texto.length() <= 300 ? texto : texto.substring(0, 297) + "...";
    }
}
