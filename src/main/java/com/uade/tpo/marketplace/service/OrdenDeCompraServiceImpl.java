package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.ordenes.OrdenDeCompraResponse;
import com.uade.tpo.marketplace.controllers.ordenes.RolEnOrden;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import com.uade.tpo.marketplace.entity.Carrito;
import com.uade.tpo.marketplace.entity.EstadoOrden;
import com.uade.tpo.marketplace.entity.EstadoPublicacion;
import com.uade.tpo.marketplace.entity.ItemCarrito;
import com.uade.tpo.marketplace.entity.OrderDetail;
import com.uade.tpo.marketplace.entity.OrdenDeCompra;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.CambioDeEstadoNoPermitidoException;
import com.uade.tpo.marketplace.exceptions.CarritoVacioException;
import com.uade.tpo.marketplace.exceptions.CompraPropiaException;
import com.uade.tpo.marketplace.exceptions.AdminNoComerciaException;
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

/**
 * Logica de ordenes: convierte un carrito en una compra cerrada.
 *
 * Lo llama OrdenesController. Lee el carrito, valida que tenga items y stock,
 * copia cada linea a un OrderDetail con el precio del momento, descuenta el
 * stock del producto y vacia el carrito.
 */
@Service
@RequiredArgsConstructor
public class OrdenDeCompraServiceImpl implements OrdenDeCompraService {

    private static final EstadoOrden ESTADO_INICIAL = EstadoOrden.PENDIENTE;

    private final OrdenDeCompraRepository ordenRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CarritoService carritoService;
    private final AutorizacionService autorizacion;
    private final EntityManager entityManager;

    /**
     * Un cliente no ve las ordenes de otros: una orden es una transaccion
     * entre dos personas y a nadie mas le incumbe. El ADMIN es la excepcion,
     * para poder auditarlas, pero si manda rol vuelve a mirarse como
     * participante y salen sus propias compras o ventas, que es lo unico que
     * ese filtro puede querer decir.
     *
     * Pre : el id de quien pregunta y, opcionalmente, desde que lado mirar.
     * Post: las ordenes donde participa. Sin rol, las dos puntas; un ADMIN sin
     *       rol recibe todas las del sistema. Tira 404 si el usuario no
     *       existe, para no confundirlo con uno real que todavia no tiene
     *       movimientos: los dos casos darian una lista vacia.
     */
    public List<OrdenDeCompraResponse> getOrdenes(Long idSolicitante, RolEnOrden rol)
            throws UsuarioNoEncontradoException {
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

        // Sin esto filtrar el listado no serviria de nada: bastaria con pedir
        // las ordenes de a una por id para leer las de cualquier otro.
        boolean esParte = orden.getComprador().getId().equals(idSolicitante)
                || orden.getVendedor().getId().equals(idSolicitante);
        if (!esParte && !autorizacion.esAdmin(idSolicitante))
            throw new OperacionAjenaException();

        return OrdenDeCompraResponse.from(orden);
    }

    /**
     * Sin esto, filtrar por un id que no existe devuelve una lista vacia, igual
     * que un usuario real sin movimientos. Son dos situaciones distintas y el
     * cliente no tiene como distinguirlas.
     */
    private void validarQueExista(Long idUsuario) throws UsuarioNoEncontradoException {
        if (!usuarioRepository.existsById(idUsuario))
            throw new UsuarioNoEncontradoException();
    }

    /**
     * Cierra el carrito y lo convierte en ordenes.
     *
     * Una orden es una transaccion entre dos personas, asi que si el carrito
     * mezcla productos de varios vendedores se genera una orden por cada uno.
     * Primero se valida todo y recien despues se escribe: si un solo item falla,
     * no queda ninguna orden a medio crear.
     *
     * Pre : el id de quien compra; el contenido sale de su carrito.
     * Post: una orden por cada vendedor involucrado, con el stock ya
     *       descontado y el carrito vacio. Valida todo antes de escribir, asi
     *       que si un item falla no queda ninguna orden a medias.
     */
    @Transactional
    public List<OrdenDeCompraResponse> createOrden(Long idSolicitante)
            throws UsuarioNoEncontradoException, CarritoVacioException, StockInsuficienteException,
            ProductoNoEncontradoException, CompraPropiaException, CuentaInactivaException, AdminNoComerciaException {
        autorizacion.validarActivo(idSolicitante);
        autorizacion.validarQueNoSeaAdmin(idSolicitante);

        Carrito carrito = carritoService.obtenerCarritoEntidad(idSolicitante);

        if (carrito.getItems().isEmpty())
            throw new CarritoVacioException();

        // Se toman los candados antes de mirar nada, y siempre en el mismo
        // orden: si dos compras coinciden en varios productos, ordenarlos por
        // id evita que cada una se quede con la mitad de lo que la otra
        // necesita, que es como se armaba el interbloqueo.
        //
        // Va un refresh y no un find: al cargar el carrito los productos ya
        // quedaron en memoria, y pedirlos de nuevo devolveria esa copia con el
        // stock de antes de esperar el candado. Asi cada compra vuelve a leer
        // la fila recien liberada por la anterior.
        for (Long idProducto : carrito.getItems().stream()
                .map(item -> item.getProducto().getId())
                .distinct().sorted().toList()) {
            Producto producto = productoRepository.findById(idProducto)
                    .orElseThrow(ProductoNoEncontradoException::new);
            entityManager.refresh(producto, LockModeType.PESSIMISTIC_WRITE);
        }

        for (ItemCarrito item : carrito.getItems()) {
            Producto producto = item.getProducto();
            // El producto pudo darse de baja o pausarse despues de entrar al carrito.
            if (!producto.getActivo()
                    || producto.getEstadoPublicacion() != EstadoPublicacion.PUBLICADO)
                throw new ProductoNoEncontradoException();
            // Se repite el chequeo del carrito porque este es el punto donde se
            // descuenta el stock y se escribe la orden: un item cargado antes de
            // que existiera la regla llegaria hasta aca sin que nadie lo mire.
            if (producto.getVendedor().getId().equals(idSolicitante))
                throw new CompraPropiaException(producto);
            if (producto.getStock() < item.getCantidad())
                throw new StockInsuficienteException(producto, item.getCantidad());
        }

        // LinkedHashMap para que las ordenes salgan en el mismo orden en que el
        // comprador fue cargando los productos.
        Map<Usuario, List<ItemCarrito>> porVendedor = new LinkedHashMap<>();
        for (ItemCarrito item : carrito.getItems())
            porVendedor.computeIfAbsent(item.getProducto().getVendedor(), v -> new ArrayList<>())
                    .add(item);

        List<OrdenDeCompraResponse> ordenes = new ArrayList<>();
        for (Map.Entry<Usuario, List<ItemCarrito>> entrada : porVendedor.entrySet())
            ordenes.add(OrdenDeCompraResponse.from(
                    armarOrden(carrito.getUsuario(), entrada.getKey(), entrada.getValue())));

        // El carrito es unico por usuario y se reutiliza: se vacia tras la compra.
        carritoService.vaciarEntidad(idSolicitante);
        return ordenes;
    }

    /**
     * Arma y guarda la orden de un vendedor con los items que le corresponden.
     *
     * Pre : el comprador, el vendedor y los items que le corresponden.
     * Post: la orden guardada, con cada renglon copiando el precio del momento
     *       para que una edicion posterior no reescriba la historia.
     */
    private OrdenDeCompra armarOrden(Usuario comprador, Usuario vendedor, List<ItemCarrito> items) {
        OrdenDeCompra orden = new OrdenDeCompra();
        orden.setComprador(comprador);
        orden.setVendedor(vendedor);
        orden.setEstado(ESTADO_INICIAL);

        LocalDateTime ahora = LocalDateTime.now();
        orden.setFechaCreacion(ahora);
        orden.setFechaUltimoEstado(ahora);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (ItemCarrito item : items) {
            Producto producto = item.getProducto();

            // Copia de los datos del producto tal como estan ahora. A partir de
            // aca la orden es independiente: cambiar el producto no la altera.
            OrderDetail renglon = new OrderDetail();
            renglon.setOrden(orden);
            renglon.setProducto(producto);
            renglon.setNombre(producto.getNombre());
            renglon.setCantidad(item.getCantidad());
            renglon.setPrecioUnitario(producto.getPrecio());
            renglon.setDescuento(producto.getDescuento() == null ? 0 : producto.getDescuento());
            orden.getItems().add(renglon);

            subtotal = subtotal.add(renglon.getSubtotal());
            total = total.add(renglon.getTotal());

            producto.setStock(producto.getStock() - item.getCantidad());
            producto.setVendidos(producto.getVendidos() + item.getCantidad());
            productoRepository.save(producto);
        }

        orden.setSubtotal(subtotal);
        orden.setTotal(total);
        return ordenRepository.save(orden);
    }

    /**
     * Avanza el estado de una orden.
     *
     * Mientras no haya autenticacion, quien pide el cambio llega como parametro
     * desde el controller. Cuando se sume el token, el idSolicitante sale de ahi y
     * las validaciones no cambian.
     *
     * Pre : el id de la orden, el estado destino y el id de quien pide.
     * Post: la orden en el estado nuevo. Cancelar repone el stock. Tira
     *       CambioDeEstadoNoPermitidoException si el paso no le toca a quien
     *       lo pide, y TransicionInvalidaException si el salto no existe.
     */
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

        // La transicion se valida siempre, incluso para el ADMIN: que un salto
        // no exista no es una cuestion de permisos sino de que la orden quedaria
        // en un estado que no significa nada.
        validarTransicion(orden.getEstado(), estado);

        // Quien pide, en cambio, si es cuestion de permisos, y ahi el ADMIN
        // pasa. Es el unico que puede marcar PAGADA: sin pasarela de pago,
        // ninguna de las dos partes puede demostrar que el dinero entro.
        if (!esAdmin)
            validarQuienPuede(estado, esComprador, esVendedor);

        // Cancelar tiene que devolver lo que la compra habia reservado. Sin
        // esto, cancelar una orden dejaba el stock descontado para siempre y el
        // vendedor perdia unidades que nunca vendio.
        if (estado == EstadoOrden.CANCELADA)
            reponerStock(orden);

        orden.setEstado(estado);
        orden.setFechaUltimoEstado(LocalDateTime.now());
        return OrdenDeCompraResponse.from(ordenRepository.save(orden));
    }

    /**
     * Devuelve al producto las unidades que la orden habia descontado.
     *
     * Usa la cantidad guardada en el OrderDetail, no la del carrito: el carrito
     * ya se vacio cuando se cerro la compra.
     *
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
     * El flujo es PENDIENTE -> PAGADA, y desde PENDIENTE tambien se puede
     * CANCELAR. La orden no sigue a la mercaderia: despachar y entregar son
     * hechos del envio.
     *
     * PAGADA y CANCELADA son finales. Que de PAGADA no se salga es una regla
     * de la maquina, no de permisos, asi que alcanza tambien al ADMIN: cancelar
     * un cobro que ya ocurrio no es un cambio de estado sino una devolucion, y
     * eso todavia no existe en el sistema.
     *
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
     * Lo unico que puede declarar una de las partes es CANCELADA, y solo llega
     * aca si la orden estaba PENDIENTE, porque la transicion desde PAGADA ya la
     * corto validarTransicion.
     *
     * PAGADA no es de nadie. Que el dinero entro es un hecho de un tercero, no
     * de las partes: el comprador tiene motivo para decir que pago sin haber
     * pagado, y el vendedor no tiene forma de probarlo dentro del sistema.
     * Hasta que exista una pasarela que lo confirme por webhook, la marca el
     * ADMIN a mano, que es quien puede mirar el comprobante. Cuando esa
     * pasarela exista, PAGADA deja de ser algo que alguien pide y pasa a ser
     * algo que el sistema escribe solo.
     *
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
}
