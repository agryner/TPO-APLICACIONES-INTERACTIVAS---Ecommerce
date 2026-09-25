package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.ordenes.OrdenDeCompraResponse;
import com.uade.tpo.marketplace.controllers.ordenes.OrdenRequest;
import com.uade.tpo.marketplace.controllers.ordenes.RolEnOrden;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import com.uade.tpo.marketplace.entity.Carrito;
import com.uade.tpo.marketplace.entity.DireccionEntrega;
import com.uade.tpo.marketplace.entity.EstadoOrden;
import com.uade.tpo.marketplace.entity.EstadoPublicacion;
import com.uade.tpo.marketplace.entity.MetodoEntrega;
import com.uade.tpo.marketplace.entity.ItemCarrito;
import com.uade.tpo.marketplace.entity.OrderDetail;
import com.uade.tpo.marketplace.entity.OrdenDeCompra;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.entity.TipoNotificacion;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.CambioDeEstadoNoPermitidoException;
import com.uade.tpo.marketplace.exceptions.CarritoVacioException;
import com.uade.tpo.marketplace.exceptions.CompraPropiaException;
import com.uade.tpo.marketplace.exceptions.DireccionDeEntregaRequeridaException;
import com.uade.tpo.marketplace.exceptions.RolNoComerciaException;
import com.uade.tpo.marketplace.exceptions.OrdenNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.StockInsuficienteException;
import com.uade.tpo.marketplace.exceptions.TransicionInvalidaException;
import com.uade.tpo.marketplace.repository.OrdenDeCompraRepository;
import com.uade.tpo.marketplace.repository.ProductoRepository;
import com.uade.tpo.marketplace.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;

@Service
@RequiredArgsConstructor
public class OrdenDeCompraServiceImpl implements OrdenDeCompraService {
    private static final EstadoOrden ESTADO_INICIAL = EstadoOrden.PENDIENTE;

    private final OrdenDeCompraRepository ordenRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CarritoService carritoService;
    private final AutorizacionService autorizacion;
    private final NotificacionService notificacionService;
    private final EnvioService envioService;

    @Value("${marketplace.notificaciones.stock-bajo:2}")
    private int stockBajo;
    private final EntityManager entityManager;

    /**
     * Pre : el id de quien pregunta y, opcionalmente, desde que lado mirar.
     * Post: las ordenes donde participa. Sin rol, las dos puntas; un ADMIN sin
     *       rol recibe todas las del sistema. Tira 404 si el usuario no
     *       existe, para no confundirlo con uno real que todavia no tiene
     *       movimientos: los dos casos darian una lista vacia.
     */
    public List<OrdenDeCompraResponse> getOrdenes(Long idSolicitante, RolEnOrden rol)
            throws UsuarioNoEncontradoException, SinResultadosException {
        validarQueExista(idSolicitante);

        List<OrdenDeCompra> propias;
        if (rol == RolEnOrden.COMPRADOR)
            propias = ordenRepository.findByCompradorId(idSolicitante);
        else if (rol == RolEnOrden.VENDEDOR)
            propias = ordenRepository.findByVendedorId(idSolicitante);
        else if (autorizacion.esAdmin(idSolicitante))
            propias = ordenRepository.findAll();
        else
            propias = ordenRepository.findByCompradorIdOrVendedorId(idSolicitante, idSolicitante);

        if (propias.isEmpty())
            throw new SinResultadosException("Todavia no tenes ordenes");

        return propias.stream()
                .map(OrdenDeCompraResponse::from)
                .toList();
    }

    /**
     * Pre : el id de la orden y el id de quien pregunta.
     * Post: la orden. Tira OperacionAjenaException si no es ni comprador ni
     *       vendedor ni ADMIN: sin esto, filtrar el listado no serviria de
     *       nada porque se podrian pedir de a una.
     */
    public OrdenDeCompraResponse getOrdenById(Long idOrden, Long idSolicitante)
            throws OrdenNoEncontradaException, OperacionAjenaException {
        OrdenDeCompra orden = ordenRepository.findById(idOrden)
                .orElseThrow(OrdenNoEncontradaException::new);

        boolean esParte = orden.getComprador().getId().equals(idSolicitante)
                || orden.getVendedor().getId().equals(idSolicitante);
        if (!esParte && !autorizacion.esAdmin(idSolicitante))
            throw new OperacionAjenaException();

        return OrdenDeCompraResponse.from(orden);
    }

    private void validarQueExista(Long idUsuario) throws UsuarioNoEncontradoException {
        if (!usuarioRepository.existsById(idUsuario))
            throw new UsuarioNoEncontradoException();
    }

    /**
     * Pre : el id de quien compra; el contenido sale de su carrito.
     * Post: una orden por cada vendedor involucrado, con el stock ya
     *       descontado y el carrito vacio. Valida todo antes de escribir, asi
     *       que si un item falla no queda ninguna orden a medias. La direccion
     *       de entrega viene en el request y se exige solo si algo se despacha:
     *       una compra toda a coordinar no la necesita.
     */
    @Transactional
    public List<OrdenDeCompraResponse> createOrden(Long idSolicitante, OrdenRequest request)
            throws UsuarioNoEncontradoException, CarritoVacioException, StockInsuficienteException,
            ProductoNoEncontradoException, CompraPropiaException, CuentaInactivaException,
            RolNoComerciaException, DireccionDeEntregaRequeridaException {
        autorizacion.validarActivo(idSolicitante);
        autorizacion.validarQuePuedaComerciar(idSolicitante);

        Carrito carrito = carritoService.obtenerCarritoEntidad(idSolicitante);

        if (carrito.getItems().isEmpty())
            throw new CarritoVacioException();

        for (Long idProducto : carrito.getItems().stream()
                .map(item -> item.getProducto().getId())
                .distinct().sorted().toList()) {
            Producto producto = productoRepository.findById(idProducto)
                    .orElseThrow(ProductoNoEncontradoException::new);
            entityManager.refresh(producto, LockModeType.PESSIMISTIC_WRITE);
        }

        for (ItemCarrito item : carrito.getItems()) {
            Producto producto = item.getProducto();
            if (!producto.getActivo()
                    || producto.getEstadoPublicacion() != EstadoPublicacion.PUBLICADO)
                throw new ProductoNoEncontradoException();
            if (producto.getVendedor().getId().equals(idSolicitante))
                throw new CompraPropiaException(producto);
            if (producto.getStock() < item.getCantidad())
                throw new StockInsuficienteException(producto, item.getCantidad());
        }

        // Una orden es una transaccion entre dos personas con un metodo de
        // entrega. Un tractor y unas semillas del mismo vendedor salen en dos
        // ordenes: la primera se coordina y la segunda se despacha.
        Map<Grupo, List<ItemCarrito>> porGrupo = new LinkedHashMap<>();
        boolean coordinarConVendedor = Boolean.TRUE.equals(request.getCoordinarConVendedor());
        for (ItemCarrito item : carrito.getItems()) {
            Producto producto = item.getProducto();
            MetodoEntrega metodo = metodoPara(producto, coordinarConVendedor);
            porGrupo.computeIfAbsent(new Grupo(producto.getVendedor().getId(), metodo),
                    g -> new ArrayList<>()).add(item);
        }

        // La direccion se exige solo si algo se va a despachar. Una compra toda
        // a coordinar no la necesita, y pedirsela seria pedir un dato que el
        // sistema despues no usa para nada.
        DireccionEntrega entrega = new DireccionEntrega(request.getProvinciaEntrega(),
                request.getLocalidadEntrega(), request.getDireccionEntrega());
        boolean hayDespacho = porGrupo.keySet().stream()
                .anyMatch(g -> g.metodo() == MetodoEntrega.DESPACHO);

        if (hayDespacho && !entrega.estaCompleta())
            throw new DireccionDeEntregaRequeridaException();

        List<OrdenDeCompraResponse> ordenes = new ArrayList<>();
        for (Map.Entry<Grupo, List<ItemCarrito>> entrada : porGrupo.entrySet()) {
            Usuario vendedor = entrada.getValue().get(0).getProducto().getVendedor();
            List<Renglon> renglones = entrada.getValue().stream()
                    .map(i -> new Renglon(i.getProducto(), i.getCantidad(),
                            i.getProducto().getPrecio(),
                            i.getProducto().getDescuento() == null
                                    ? 0
                                    : i.getProducto().getDescuento()))
                    .toList();
            ordenes.add(OrdenDeCompraResponse.from(armarOrden(carrito.getUsuario(), vendedor,
                    renglones, entrada.getKey().metodo(), entrega)));
        }

        carritoService.vaciarEntidad(idSolicitante);
        return ordenes;
    }

    /**
     * Pre : el producto y si el comprador prefirio coordinar.
     * Post: el metodo que le toca. Un producto que no admite envio se coordina
     *       si o si: la preferencia del comprador no puede volver despachable
     *       una camioneta.
     */
    private MetodoEntrega metodoPara(Producto producto, boolean coordinarConVendedor) {
        if (coordinarConVendedor || !Boolean.TRUE.equals(producto.getAdmiteEnvio()))
            return MetodoEntrega.COORDINAR;

        return MetodoEntrega.DESPACHO;
    }

    private record Grupo(Long idVendedor, MetodoEntrega metodo) {
    }

    /**
     * Una linea de una orden por armar, sin importar de donde salio. El carrito
     * la construye con el precio de lista del producto; una oferta aceptada,
     * con el precio que acordaron las partes.
     */
    private record Renglon(Producto producto, int cantidad, BigDecimal precioUnitario,
            int descuento) {
    }

    /**
     * Pre : el comprador, el producto, la cantidad y el precio que se acordo.
     * Post: la orden creada y ya guardada. No pasa por el carrito: una oferta
     *       aceptada es una venta cerrada entre dos personas por un precio que
     *       no es el de lista. El metodo de entrega sale de la misma regla que
     *       el checkout: despacho si el producto lo admite, coordinar si no.
     *       La direccion es la que el comprador puso al ofertar, no la de su
     *       perfil. Tira StockInsuficienteException si al aceptar ya no hay
     *       unidades.
     */
    @Transactional
    public OrdenDeCompraResponse crearDesdeOferta(Usuario comprador, Producto producto,
            int cantidad, BigDecimal precioAcordado, DireccionEntrega entrega)
            throws StockInsuficienteException {
        if (producto.getStock() == null || producto.getStock() < cantidad)
            throw new StockInsuficienteException(producto, cantidad);

        MetodoEntrega metodo = metodoPara(producto, false);
        Renglon renglon = new Renglon(producto, cantidad, precioAcordado, 0);

        return OrdenDeCompraResponse.from(armarOrden(comprador, producto.getVendedor(),
                List.of(renglon), metodo, entrega));
    }

    /**
     * Pre : el comprador, el vendedor, los items que le corresponden, el metodo
     *       de entrega y a donde va.
     * Post: la orden guardada, con cada renglon copiando el precio del momento
     *       para que una edicion posterior no reescriba la historia. La
     *       direccion se guarda solo si se despacha.
     */
    private OrdenDeCompra armarOrden(Usuario comprador, Usuario vendedor, List<Renglon> items,
            MetodoEntrega metodo, DireccionEntrega entrega) {
        OrdenDeCompra orden = new OrdenDeCompra();
        orden.setComprador(comprador);
        orden.setVendedor(vendedor);
        orden.setEstado(ESTADO_INICIAL);
        orden.setMetodoEntrega(metodo);
        // Solo en las que se despachan: en una coordinada quedaria un dato que
        // nadie usa, y es la direccion de alguien.
        orden.setEntrega(metodo == MetodoEntrega.DESPACHO ? entrega : new DireccionEntrega());

        LocalDateTime ahora = LocalDateTime.now();
        orden.setFechaCreacion(ahora);
        orden.setFechaUltimoEstado(ahora);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (Renglon item : items) {
            Producto producto = item.producto();

            OrderDetail renglon = new OrderDetail();
            renglon.setOrden(orden);
            renglon.setProducto(producto);
            renglon.setNombre(producto.getNombre());
            renglon.setCantidad(item.cantidad());
            renglon.setPrecioUnitario(item.precioUnitario());
            renglon.setDescuento(item.descuento());
            orden.getItems().add(renglon);

            subtotal = subtotal.add(renglon.getSubtotal());
            total = total.add(renglon.getTotal());

            producto.setStock(producto.getStock() - item.cantidad());
            avisarSiQuedaPoco(producto);
            producto.setVendidos(producto.getVendidos() + item.cantidad());
            productoRepository.save(producto);
        }

        orden.setSubtotal(subtotal);
        orden.setTotal(total);
        return ordenRepository.save(orden);
    }

    /**
     * Pre : el id de la orden, el estado destino y el id de quien pide.
     * Post: la orden en el estado nuevo. Cancelar repone el stock. Tira
     *       CambioDeEstadoNoPermitidoException si el paso no le toca a quien
     *       lo pide, y TransicionInvalidaException si el salto no existe.
     */
    @Transactional
    public OrdenDeCompraResponse actualizarEstado(Long idOrden, EstadoOrden estado, Long idSolicitante)
            throws OrdenNoEncontradaException, TransicionInvalidaException,
            CambioDeEstadoNoPermitidoException {
        OrdenDeCompra orden = ordenRepository.findById(idOrden)
                .orElseThrow(OrdenNoEncontradaException::new);

        boolean esComprador = orden.getComprador().getId().equals(idSolicitante);
        boolean esVendedor = orden.getVendedor().getId().equals(idSolicitante);
        boolean esAdmin = autorizacion.esAdmin(idSolicitante);

        if (!esComprador && !esVendedor && !esAdmin)
            throw new CambioDeEstadoNoPermitidoException();

        validarTransicion(orden.getEstado(), estado);

        if (!esAdmin)
            validarQuienPuede(estado, esComprador, esVendedor);

        if (estado == EstadoOrden.CANCELADA)
            reponerStock(orden);

        orden.setEstado(estado);
        orden.setFechaUltimoEstado(LocalDateTime.now());
        OrdenDeCompra guardada = ordenRepository.save(orden);

        // El envio nace recien cuando la orden esta paga: no se despacha algo
        // que todavia no se cobro.
        if (estado == EstadoOrden.PAGADA)
            envioService.crearParaOrden(guardada);

        return OrdenDeCompraResponse.from(guardada);
    }

    /**
     * Pre : la orden que se esta cancelando.
     * Post: nada. Devuelve al producto las unidades de cada renglon.
     */
    private void reponerStock(OrdenDeCompra orden) {
        for (OrderDetail item : orden.getItems()) {
            Producto producto = item.getProducto();
            if (producto == null)
                continue;
            producto.setStock(producto.getStock() + item.getCantidad());
            producto.setVendidos(Math.max(0, producto.getVendidos() - item.getCantidad()));
            productoRepository.save(producto);
        }
    }

    /**
     * Pre : el estado actual y el destino.
     * Post: nada si el salto existe.
     */
    private void validarTransicion(EstadoOrden actual, EstadoOrden nuevo)
            throws TransicionInvalidaException {
        boolean permitida = switch (actual) {
            case PENDIENTE -> nuevo == EstadoOrden.PAGADA || nuevo == EstadoOrden.CANCELADA;
            case PAGADA, CANCELADA -> false;
        };

        if (!permitida)
            throw new TransicionInvalidaException();
    }

    /**
     * Pre : el estado destino y si quien pide es el comprador o el vendedor.
     *       No se llama para el ADMIN.
     * Post: nada si le toca. Solo CANCELADA es de las partes.
     */
    private void validarQuienPuede(EstadoOrden nuevo, boolean esComprador, boolean esVendedor)
            throws CambioDeEstadoNoPermitidoException {
        boolean autorizado = switch (nuevo) {
            case CANCELADA -> esComprador || esVendedor;
            case PAGADA, PENDIENTE -> false;
        };

        if (!autorizado)
            throw new CambioDeEstadoNoPermitidoException();
    }

    /**
     * Pre : el producto con el stock ya descontado.
     * Post: nada. Si quedo en el umbral o por debajo, le avisa al vendedor
     *       para que reponga y a los que lo tienen guardado para que sepan que
     *       quedan pocos. Se dispara solo al comprar, que es el unico momento
     *       en que el stock baja sin que nadie lo edite.
     */
    private void avisarSiQuedaPoco(Producto producto) {
        if (producto.getStock() == null || producto.getStock() > stockBajo)
            return;

        notificacionService.crear(producto.getVendedor(), TipoNotificacion.POCO_STOCK,
                "Te quedan %d unidades de \"%s\"".formatted(
                        producto.getStock(), producto.getNombre()),
                "/productos/" + producto.getId());

        notificacionService.avisarAQuienesLoTienenGuardado(producto, TipoNotificacion.POCO_STOCK,
                "Quedan %d unidades de \"%s\"".formatted(
                        producto.getStock(), producto.getNombre()));
    }
}
