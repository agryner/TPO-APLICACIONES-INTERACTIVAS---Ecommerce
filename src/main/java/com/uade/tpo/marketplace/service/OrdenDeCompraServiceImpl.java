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

        Map<Usuario, List<ItemCarrito>> porVendedor = new LinkedHashMap<>();
        for (ItemCarrito item : carrito.getItems())
            porVendedor.computeIfAbsent(item.getProducto().getVendedor(), v -> new ArrayList<>())
                    .add(item);

        List<OrdenDeCompraResponse> ordenes = new ArrayList<>();
        for (Map.Entry<Usuario, List<ItemCarrito>> entrada : porVendedor.entrySet())
            ordenes.add(OrdenDeCompraResponse.from(
                    armarOrden(carrito.getUsuario(), entrada.getKey(), entrada.getValue())));

        carritoService.vaciarEntidad(idSolicitante);
        return ordenes;
    }

    /**
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

        validarTransicion(orden.getEstado(), estado);

        if (!esAdmin)
            validarQuienPuede(estado, esComprador, esVendedor);

        if (estado == EstadoOrden.CANCELADA)
            reponerStock(orden);

        orden.setEstado(estado);
        orden.setFechaUltimoEstado(LocalDateTime.now());
        return OrdenDeCompraResponse.from(ordenRepository.save(orden));
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
}
