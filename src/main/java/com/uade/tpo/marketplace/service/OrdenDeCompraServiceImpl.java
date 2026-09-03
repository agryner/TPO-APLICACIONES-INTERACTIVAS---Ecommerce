package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.entity.dto.OrdenDeCompraResponse;
import com.uade.tpo.marketplace.entity.dto.RolEnOrden;
import java.math.BigDecimal;
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
     */
    @Transactional
    public List<OrdenDeCompraResponse> createOrden(Long idSolicitante)
            throws UsuarioNoEncontradoException, CarritoVacioException, StockInsuficienteException,
            ProductoNoEncontradoException, CompraPropiaException, CuentaInactivaException {
        autorizacion.validarActivo(idSolicitante);

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

    /** Arma y guarda la orden de un vendedor con los items que le corresponden. */
    private OrdenDeCompra armarOrden(Usuario comprador, Usuario vendedor, List<ItemCarrito> items) {
        OrdenDeCompra orden = new OrdenDeCompra();
        orden.setComprador(comprador);
        orden.setVendedor(vendedor);
        orden.setEstado(ESTADO_INICIAL);

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
     */
    public OrdenDeCompraResponse actualizarEstado(Long idOrden, EstadoOrden estado, Long idSolicitante)
            throws OrdenNoEncontradaException, TransicionInvalidaException,
            CambioDeEstadoNoPermitidoException {
        OrdenDeCompra orden = ordenRepository.findById(idOrden)
                .orElseThrow(OrdenNoEncontradaException::new);

        boolean esComprador = orden.getComprador().getId().equals(idSolicitante);
        boolean esVendedor = orden.getVendedor().getId().equals(idSolicitante);
        if (!esComprador && !esVendedor)
            throw new CambioDeEstadoNoPermitidoException();

        validarTransicion(orden.getEstado(), estado);
        validarQuienPuede(estado, esComprador, esVendedor);

        // Cancelar tiene que devolver lo que la compra habia reservado. Sin
        // esto, cancelar una orden dejaba el stock descontado para siempre y el
        // vendedor perdia unidades que nunca vendio.
        if (estado == EstadoOrden.CANCELADA)
            reponerStock(orden);

        orden.setEstado(estado);
        return OrdenDeCompraResponse.from(ordenRepository.save(orden));
    }

    /**
     * El flujo es PENDIENTE -> PAGADA -> ENVIADA -> RECIBIDA, y se puede
     * cancelar mientras no haya salido el envio. RECIBIDA y CANCELADA no tienen
     * salida: una vez ahi la orden esta cerrada.
     */
    /**
     * Devuelve al producto las unidades que la orden habia descontado.
     *
     * Usa la cantidad guardada en el OrderDetail, no la del carrito: el carrito
     * ya se vacio cuando se cerro la compra.
     */
    private void reponerStock(OrdenDeCompra orden) {
        for (OrderDetail item : orden.getItems()) {
            Producto producto = item.getProducto();
            if (producto == null)
                continue;
            producto.setStock(producto.getStock() + item.getCantidad());
            productoRepository.save(producto);
        }
    }

    private void validarTransicion(EstadoOrden actual, EstadoOrden nuevo)
            throws TransicionInvalidaException {
        boolean permitida = switch (actual) {
            case PENDIENTE -> nuevo == EstadoOrden.PAGADA || nuevo == EstadoOrden.CANCELADA;
            case PAGADA -> nuevo == EstadoOrden.ENVIADA || nuevo == EstadoOrden.CANCELADA;
            case ENVIADA -> nuevo == EstadoOrden.RECIBIDA;
            case RECIBIDA, CANCELADA -> false;
        };

        if (!permitida)
            throw new TransicionInvalidaException();
    }

    /**
     * Cada paso lo declara quien puede saberlo de verdad: el vendedor es el que
     * despacha y el comprador el que paga y el que recibe. Cancelar lo puede
     * pedir cualquiera de los dos.
     */
    private void validarQuienPuede(EstadoOrden nuevo, boolean esComprador, boolean esVendedor)
            throws CambioDeEstadoNoPermitidoException {
        boolean autorizado = switch (nuevo) {
            case PAGADA, RECIBIDA -> esComprador;
            case ENVIADA -> esVendedor;
            case CANCELADA -> esComprador || esVendedor;
            case PENDIENTE -> false;
        };

        if (!autorizado)
            throw new CambioDeEstadoNoPermitidoException();
    }
}
