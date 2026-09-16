package com.uade.tpo.marketplace.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.marketplace.controllers.ofertas.OfertaRequest;
import com.uade.tpo.marketplace.controllers.ofertas.OfertaResponse;
import com.uade.tpo.marketplace.controllers.productos.ProductoResumenResponse;
import com.uade.tpo.marketplace.entity.EstadoOferta;
import com.uade.tpo.marketplace.entity.Oferta;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.entity.TipoNotificacion;
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
import com.uade.tpo.marketplace.repository.OfertaRepository;
import com.uade.tpo.marketplace.repository.ProductoRepository;
import com.uade.tpo.marketplace.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OfertaServiceImpl implements OfertaService {
    private final OfertaRepository ofertaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AutorizacionService autorizacion;
    private final NotificacionService notificacionService;
    private final OrdenDeCompraService ordenService;

    @Value("${marketplace.ofertas.dias-vigencia:7}")
    private long diasVigencia;

    /**
     * Pre : el request con producto, cantidad y precio por unidad, mas el id de
     *       quien ofrece.
     * Post: la oferta pendiente, y una notificacion al vendedor. No reserva
     *       stock: si lo hiciera, cualquiera podria bloquear el inventario de
     *       un vendedor gratis. El stock se valida recien al aceptar.
     */
    @Transactional
    public OfertaResponse crear(OfertaRequest request, Long idSolicitante)
            throws ProductoNoEncontradoException, CompraPropiaException,
            PrecioOfrecidoInvalidoException, OfertaDuplicadaException,
            ProductoNoAceptaOfertasException, UsuarioNoEncontradoException,
            CuentaInactivaException, RolNoComerciaException {
        autorizacion.validarActivo(idSolicitante);
        autorizacion.validarQuePuedaComerciar(idSolicitante);

        Producto producto = productoRepository.findById(request.getIdProducto())
                .filter(ProductoResumenResponse::estaDisponible)
                .orElseThrow(ProductoNoEncontradoException::new);

        if (producto.getVendedor().getId().equals(idSolicitante))
            throw new CompraPropiaException(producto);

        // No todo se negocia: el vendedor decide producto por producto.
        if (!Boolean.TRUE.equals(producto.getAceptaOfertas()))
            throw new ProductoNoAceptaOfertasException();

        // Ofrecer el precio de lista o mas no es negociar: para eso esta el
        // boton de comprar.
        if (request.getPrecioOfrecido()
                .compareTo(ProductoResumenResponse.conDescuento(producto)) >= 0)
            throw new PrecioOfrecidoInvalidoException();

        if (ofertaRepository.findByCompradorIdAndProductoIdAndEstado(
                idSolicitante, producto.getId(), EstadoOferta.PENDIENTE).isPresent())
            throw new OfertaDuplicadaException();

        Usuario comprador = usuarioRepository.findById(idSolicitante)
                .orElseThrow(UsuarioNoEncontradoException::new);

        LocalDateTime ahora = LocalDateTime.now();

        Oferta oferta = new Oferta();
        oferta.setProducto(producto);
        oferta.setComprador(comprador);
        oferta.setCantidad(request.getCantidad());
        oferta.setPrecioOfrecido(request.getPrecioOfrecido());
        oferta.setEstado(EstadoOferta.PENDIENTE);
        oferta.setFechaCreacion(ahora);
        oferta.setFechaVencimiento(ahora.plusDays(diasVigencia));

        Oferta guardada = ofertaRepository.save(oferta);

        notificacionService.crear(producto.getVendedor(), TipoNotificacion.OFERTA_RECIBIDA,
                "Tenes una oferta de $%s por %d unidad(es) de \"%s\"".formatted(
                        request.getPrecioOfrecido().toPlainString(), request.getCantidad(),
                        producto.getNombre()),
                "/ofertas");

        return OfertaResponse.from(guardada);
    }

    /**
     * Pre : el id de quien pregunta.
     * Post: las ofertas que hizo y las que recibio. Un vendedor que ademas
     *       compra ve las dos cosas en la misma lista: cada una dice de que
     *       producto es y quien la hizo.
     */
    public List<OfertaResponse> getMias(Long idSolicitante) throws SinResultadosException {
        List<Oferta> encontradas = new ArrayList<>(
                ofertaRepository.findByCompradorIdOrderByFechaCreacionDesc(idSolicitante));
        encontradas.addAll(
                ofertaRepository.findByProductoVendedorIdOrderByFechaCreacionDesc(idSolicitante));

        if (encontradas.isEmpty())
            throw new SinResultadosException("No tenes ofertas");

        return encontradas.stream().map(OfertaResponse::from).toList();
    }

    /**
     * Pre : el id de la oferta y el del vendedor que responde.
     * Post: la oferta aceptada y la orden ya creada: si el vendedor acepto, la
     *       venta esta cerrada. El comprador se entera por notificacion. Tira
     *       StockInsuficienteException si entre que la oferta se hizo y se
     *       acepto se vendieron las unidades, que es justamente por lo que la
     *       oferta no reserva nada.
     */
    @Transactional
    public OfertaResponse aceptar(Long idOferta, Long idSolicitante)
            throws OfertaNoEncontradaException, OperacionAjenaException,
            OfertaYaRespondidaException, StockInsuficienteException {
        Oferta oferta = pendienteDelVendedor(idOferta, idSolicitante);

        ordenService.crearDesdeOferta(oferta.getComprador(), oferta.getProducto(),
                oferta.getCantidad(), oferta.getPrecioOfrecido());

        oferta.setEstado(EstadoOferta.ACEPTADA);
        oferta.setFechaRespuesta(LocalDateTime.now());

        notificacionService.crear(oferta.getComprador(), TipoNotificacion.OFERTA_RESPONDIDA,
                "Aceptaron tu oferta por \"%s\": ya tenes la orden para pagar".formatted(
                        oferta.getProducto().getNombre()),
                "/ordenes");

        return OfertaResponse.from(ofertaRepository.save(oferta));
    }

    /**
     * Pre : el id de la oferta y el del vendedor que responde.
     * Post: la oferta rechazada. El comprador puede volver a ofertar mas alto:
     *       no hace falta una contraoferta, que seria duplicar toda la entidad
     *       con los roles invertidos.
     */
    @Transactional
    public OfertaResponse rechazar(Long idOferta, Long idSolicitante)
            throws OfertaNoEncontradaException, OperacionAjenaException,
            OfertaYaRespondidaException {
        Oferta oferta = pendienteDelVendedor(idOferta, idSolicitante);

        oferta.setEstado(EstadoOferta.RECHAZADA);
        oferta.setFechaRespuesta(LocalDateTime.now());

        notificacionService.crear(oferta.getComprador(), TipoNotificacion.OFERTA_RESPONDIDA,
                "Rechazaron tu oferta por \"%s\"".formatted(oferta.getProducto().getNombre()),
                "/productos/" + oferta.getProducto().getId());

        return OfertaResponse.from(ofertaRepository.save(oferta));
    }

    /**
     * Pre : el id de la oferta y el del comprador que la retira.
     * Post: la oferta cancelada. Solo la retira quien la hizo, y solo mientras
     *       siga pendiente.
     */
    @Transactional
    public OfertaResponse cancelar(Long idOferta, Long idSolicitante)
            throws OfertaNoEncontradaException, OperacionAjenaException,
            OfertaYaRespondidaException {
        Oferta oferta = ofertaRepository.findById(idOferta)
                .orElseThrow(OfertaNoEncontradaException::new);

        if (!oferta.getComprador().getId().equals(idSolicitante))
            throw new OperacionAjenaException();

        if (oferta.getEstado() != EstadoOferta.PENDIENTE)
            throw new OfertaYaRespondidaException();

        oferta.setEstado(EstadoOferta.CANCELADA);
        oferta.setFechaRespuesta(LocalDateTime.now());
        return OfertaResponse.from(ofertaRepository.save(oferta));
    }

    /**
     * Pre : nada. La llama la tarea programada.
     * Post: cuantas vencio. Una oferta sin responder no queda viva para
     *       siempre: el precio del producto cambia y el vendedor no deberia
     *       poder aceptar dentro de seis meses algo que se ofrecio hoy.
     */
    @Transactional
    public int vencerLasViejas() {
        List<Oferta> vencidas = ofertaRepository.findByEstadoAndFechaVencimientoBefore(
                EstadoOferta.PENDIENTE, LocalDateTime.now());

        for (Oferta oferta : vencidas) {
            oferta.setEstado(EstadoOferta.VENCIDA);
            ofertaRepository.save(oferta);

            notificacionService.crear(oferta.getComprador(), TipoNotificacion.OFERTA_RESPONDIDA,
                    "Tu oferta por \"%s\" vencio sin respuesta".formatted(
                            oferta.getProducto().getNombre()),
                    "/productos/" + oferta.getProducto().getId());
        }

        return vencidas.size();
    }

    /**
     * Pre : el id de la oferta y el de quien dice ser su vendedor.
     * Post: la oferta, si esta pendiente y el producto es suyo. Tira
     *       OperacionAjenaException si no vende ese producto y
     *       OfertaYaRespondidaException si ya se resolvio.
     */
    private Oferta pendienteDelVendedor(Long idOferta, Long idSolicitante)
            throws OfertaNoEncontradaException, OperacionAjenaException,
            OfertaYaRespondidaException {
        Oferta oferta = ofertaRepository.findById(idOferta)
                .orElseThrow(OfertaNoEncontradaException::new);

        Producto producto = oferta.getProducto();
        if (producto == null || producto.getVendedor() == null
                || !producto.getVendedor().getId().equals(idSolicitante))
            throw new OperacionAjenaException();

        if (oferta.getEstado() != EstadoOferta.PENDIENTE)
            throw new OfertaYaRespondidaException();

        return oferta;
    }
}
