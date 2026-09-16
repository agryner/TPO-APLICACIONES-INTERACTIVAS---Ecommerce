package com.uade.tpo.marketplace.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.marketplace.controllers.notificaciones.NotificacionResponse;
import com.uade.tpo.marketplace.entity.ItemWishlist;
import com.uade.tpo.marketplace.entity.Notificacion;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.entity.TipoNotificacion;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.NotificacionNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;
import com.uade.tpo.marketplace.repository.ItemWishlistRepository;
import com.uade.tpo.marketplace.repository.NotificacionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificacionService {
    private final NotificacionRepository notificacionRepository;
    private final ItemWishlistRepository itemWishlistRepository;

    /**
     * Pre : a quien avisarle, de que tipo, el texto y opcionalmente adonde
     *       lleva al hacer click.
     * Post: la notificacion guardada, sin leer.
     */
    @Transactional
    public void crear(Usuario destinatario, TipoNotificacion tipo, String mensaje, String link) {
        if (destinatario == null)
            return;

        Notificacion notificacion = new Notificacion();
        notificacion.setDestinatario(destinatario);
        notificacion.setTipo(tipo);
        notificacion.setMensaje(mensaje);
        notificacion.setLink(link);
        notificacion.setLeida(false);
        notificacion.setFecha(LocalDateTime.now());
        notificacionRepository.save(notificacion);
    }

    /**
     * Pre : el producto y que avisar.
     * Post: una notificacion por cada persona que lo tenga en su wishlist. Se
     *       saltea al propio vendedor: no tiene sentido avisarle de su
     *       producto por la puerta del comprador, y para lo suyo ya recibe la
     *       notificacion que le corresponde.
     */
    @Transactional
    public void avisarAQuienesLoTienenGuardado(Producto producto, TipoNotificacion tipo,
            String mensaje) {
        Long idVendedor = producto.getVendedor() == null ? null : producto.getVendedor().getId();

        for (ItemWishlist item : itemWishlistRepository.findByProductoId(producto.getId())) {
            Usuario duenio = item.getWishlist() == null ? null : item.getWishlist().getUsuario();
            if (duenio == null || duenio.getId().equals(idVendedor))
                continue;

            crear(duenio, tipo, mensaje, "/productos/" + producto.getId());
        }
    }

    /**
     * Pre : el id de quien pregunta.
     * Post: sus notificaciones, las mas nuevas primero. Tira
     *       SinResultadosException si no tiene ninguna.
     */
    public List<NotificacionResponse> getMias(Long idUsuario) throws SinResultadosException {
        List<NotificacionResponse> mias = notificacionRepository
                .findByDestinatarioIdOrderByFechaDesc(idUsuario).stream()
                .map(NotificacionResponse::from)
                .toList();

        if (mias.isEmpty())
            throw new SinResultadosException("No tenes notificaciones");

        return mias;
    }

    /**
     * Pre : el id de quien pregunta.
     * Post: cuantas tiene sin leer. Es un COUNT y no la lista entera: lo pide
     *       el badge, que solo necesita el numero.
     */
    public long contarNoLeidas(Long idUsuario) {
        return notificacionRepository.countByDestinatarioIdAndLeidaFalse(idUsuario);
    }

    /**
     * Pre : el id de la notificacion y el de quien la marca.
     * Post: la notificacion leida. Tira OperacionAjenaException si no es suya:
     *       el id es adivinable, asi que sin este chequeo cualquiera podria
     *       marcar las de otro.
     */
    @Transactional
    public NotificacionResponse marcarLeida(Long idNotificacion, Long idUsuario)
            throws NotificacionNoEncontradaException, OperacionAjenaException {
        Notificacion notificacion = notificacionRepository.findById(idNotificacion)
                .orElseThrow(NotificacionNoEncontradaException::new);

        if (notificacion.getDestinatario() == null
                || !notificacion.getDestinatario().getId().equals(idUsuario))
            throw new OperacionAjenaException();

        notificacion.setLeida(true);
        return NotificacionResponse.from(notificacionRepository.save(notificacion));
    }
}
