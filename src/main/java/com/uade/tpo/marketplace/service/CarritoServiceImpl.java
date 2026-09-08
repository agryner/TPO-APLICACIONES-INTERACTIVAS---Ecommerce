package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.carritos.CarritoResponse;
import com.uade.tpo.marketplace.controllers.carritos.ItemCarritoRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.marketplace.entity.Carrito;
import com.uade.tpo.marketplace.entity.EstadoPublicacion;
import com.uade.tpo.marketplace.entity.ItemCarrito;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.ItemCarritoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.StockInsuficienteException;
import com.uade.tpo.marketplace.exceptions.CompraPropiaException;
import com.uade.tpo.marketplace.exceptions.AdminNoComerciaException;
import com.uade.tpo.marketplace.exceptions.CantidadInvalidaException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.repository.CarritoRepository;
import com.uade.tpo.marketplace.repository.ItemCarritoRepository;
import com.uade.tpo.marketplace.repository.ProductoRepository;
import com.uade.tpo.marketplace.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CarritoServiceImpl implements CarritoService {
    private final CarritoRepository carritoRepository;
    private final ProductoRepository productoRepository;
    private final ItemCarritoRepository itemCarritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AutorizacionService autorizacion;

    @Value("${marketplace.carrito.minutos-vigencia:1440}")
    private long minutosVigencia;

    /**
     * Pre : el id del usuario y el id de quien pregunta.
     * Post: el carrito con sus items y totales. Lo crea vacio si es la primera
     *       vez y lo vacia si vencio.
     */
    @Transactional
    public CarritoResponse obtenerCarrito(Long idUsuario)
            throws UsuarioNoEncontradoException, CuentaInactivaException {
        autorizacion.validarActivo(idUsuario);
        return CarritoResponse.from(obtenerCarritoEntidad(idUsuario));
    }

    /**
     * Pre : el id del usuario.
     * Post: la entidad Carrito, no el DTO: la usan otros services que
     *       necesitan trabajar sobre los items. Lo crea si no existe y lo
     *       vacia si vencio.
     */
    @Transactional
    public Carrito obtenerCarritoEntidad(Long idUsuario) throws UsuarioNoEncontradoException {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);

        Carrito carrito = carritoRepository.findByUsuarioId(idUsuario)
                .orElseGet(() -> crearCarritoVacio(usuario));

        return vaciarSiVencio(carrito);
    }

    /**
     * Pre : el id de un producto que dejo de estar disponible.
     * Post: nada. Ese producto queda fuera de todos los carritos donde
     *       estuviera, con los totales recalculados.
     */
    @Transactional
    public void quitarDeTodosLosCarritos(Long idProducto) {
        for (ItemCarrito item : itemCarritoRepository.findByProductoId(idProducto)) {
            Carrito carrito = item.getCarrito();
            if (carrito == null)
                continue;

            carrito.getItems().remove(item);
            renovarVigencia(carrito);
            recalcularTotales(carrito);
            carritoRepository.save(carrito);
        }
    }

    @Transactional
    public void vaciarEntidad(Long idUsuario) throws UsuarioNoEncontradoException {
        vaciarCarrito(obtenerCarritoEntidad(idUsuario));
    }

    /**
     * Pre : el id del usuario, el request con idProducto y cantidad, y el id
     *       de quien pide.
     * Post: el carrito con el item y los totales recalculados. No descuenta
     *       stock: eso pasa en el checkout. Tira ProductoNoEncontradoException
     *       si no esta publicado, StockInsuficienteException si no alcanza,
     *       CompraPropiaException si el producto es tuyo.
     */
    @Transactional
    public CarritoResponse agregarItem(Long idUsuario, ItemCarritoRequest request)
            throws UsuarioNoEncontradoException,
            ProductoNoEncontradoException, StockInsuficienteException,
            CompraPropiaException, CantidadInvalidaException, CuentaInactivaException, AdminNoComerciaException {
        autorizacion.validarActivo(idUsuario);

        autorizacion.validarQueNoSeaAdmin(idUsuario);

        Carrito carrito = obtenerCarritoEntidad(idUsuario);
        Producto producto = productoRepository.findById(request.getIdProducto())
                .filter(Producto::getActivo)
                .filter(p -> p.getEstadoPublicacion() == EstadoPublicacion.PUBLICADO)
                .orElseThrow(ProductoNoEncontradoException::new);

        if (producto.getVendedor().getId().equals(idUsuario))
            throw new CompraPropiaException(producto);

        int cantidad = request.getCantidad() == null ? 1 : request.getCantidad();
        if (cantidad < 1)
            throw new CantidadInvalidaException();

        if (producto.getStock() < cantidad)
            throw new StockInsuficienteException(producto, cantidad);

        ItemCarrito item = carrito.getItems().stream()
                .filter(i -> i.getProducto().getId().equals(producto.getId()))
                .findFirst()
                .orElse(null);

        if (item == null) {
            item = new ItemCarrito();
            item.setCarrito(carrito);
            item.setProducto(producto);
            item.setCantidad(cantidad);
            carrito.getItems().add(item);
        } else {
            if (producto.getStock() < item.getCantidad() + cantidad)
                throw new StockInsuficienteException(producto, item.getCantidad() + cantidad);
            item.setCantidad(item.getCantidad() + cantidad);
        }

        renovarVigencia(carrito);
        recalcularTotales(carrito);
        return CarritoResponse.from(carritoRepository.save(carrito));
    }

    /**
     * Pre : el id del usuario, el id del item, el request con la cantidad
     *       nueva y el id de quien pide.
     * Post: el carrito actualizado. Con cantidad cero o negativa el item se
     *       elimina, que es la diferencia con agregarItem.
     */
    @Transactional
    public CarritoResponse modificarCantidad(Long idUsuario, Long idItem, Integer nuevaCantidad)
            throws UsuarioNoEncontradoException,
            ItemCarritoNoEncontradoException, StockInsuficienteException, CuentaInactivaException {
        autorizacion.validarActivo(idUsuario);

        if (nuevaCantidad == null || nuevaCantidad <= 0)
            return eliminarItem(idUsuario, idItem);

        Carrito carrito = obtenerCarritoEntidad(idUsuario);

        ItemCarrito item = carrito.getItems().stream()
                .filter(i -> i.getId().equals(idItem))
                .findFirst()
                .orElseThrow(ItemCarritoNoEncontradoException::new);

        if (item.getProducto().getStock() < nuevaCantidad)
            throw new StockInsuficienteException(item.getProducto(), nuevaCantidad);

        item.setCantidad(nuevaCantidad);

        renovarVigencia(carrito);
        recalcularTotales(carrito);
        return CarritoResponse.from(carritoRepository.save(carrito));
    }

    /**
     * Pre : el id del usuario, el id del item y el id de quien pide.
     * Post: el carrito sin ese item. Tira ItemCarritoNoEncontradoException si
     *       ese item no esta en ese carrito.
     */
    @Transactional
    public CarritoResponse eliminarItem(Long idUsuario, Long idItem)
            throws UsuarioNoEncontradoException,
            ItemCarritoNoEncontradoException, CuentaInactivaException {
        autorizacion.validarActivo(idUsuario);

        Carrito carrito = obtenerCarritoEntidad(idUsuario);

        boolean removido = carrito.getItems().removeIf(i -> i.getId().equals(idItem));
        if (!removido)
            throw new ItemCarritoNoEncontradoException();

        renovarVigencia(carrito);
        recalcularTotales(carrito);
        return CarritoResponse.from(carritoRepository.save(carrito));
    }

    /**
     * Pre : el id del usuario y el id de quien pide.
     * Post: el carrito sin items y con los totales en cero.
     */
    @Transactional
    public CarritoResponse vaciar(Long idUsuario)
            throws UsuarioNoEncontradoException, CuentaInactivaException {
        autorizacion.validarActivo(idUsuario);
        return CarritoResponse.from(vaciarCarrito(obtenerCarritoEntidad(idUsuario)));
    }

    /**
     * Pre : el usuario.
     * Post: un carrito nuevo ya guardado. Hay uno solo por persona, asi que
     *       esto corre una vez en la vida de cada cuenta.
     */
    private Carrito crearCarritoVacio(Usuario usuario) {
        Carrito carrito = new Carrito();
        carrito.setUsuario(usuario);
        carrito.setSubtotal(BigDecimal.ZERO);
        carrito.setTotal(BigDecimal.ZERO);
        return carritoRepository.save(carrito);
    }

    /**
     * Pre : el carrito.
     * Post: el mismo carrito, vacio si su fechaLimite ya paso. El chequeo es
     *       perezoso: ocurre cuando alguien lo mira, no cuando se cumple el
     *       plazo, asi que no hace falta ninguna tarea programada.
     */
    private Carrito vaciarSiVencio(Carrito carrito) {
        boolean vencio = carrito.getFechaLimite() != null
                && carrito.getFechaLimite().isBefore(LocalDateTime.now());

        return vencio ? vaciarCarrito(carrito) : carrito;
    }

    /**
     * Pre : el carrito.
     * Post: el mismo carrito sin items, con los totales en cero y sin fecha de
     *       vencimiento.
     */
    private Carrito vaciarCarrito(Carrito carrito) {
        carrito.getItems().clear();
        carrito.setFechaLimite(null);
        carrito.setSubtotal(BigDecimal.ZERO);
        carrito.setTotal(BigDecimal.ZERO);
        return carritoRepository.save(carrito);
    }

    private void renovarVigencia(Carrito carrito) {
        carrito.setFechaLimite(carrito.getItems().isEmpty()
                ? null
                : LocalDateTime.now().plusMinutes(minutosVigencia));
    }

    /**
     * Pre : el carrito.
     * Post: nada. Deja subtotal como la suma de precio por cantidad, y total
     *       como lo mismo aplicando el descuento de cada producto.
     */
    private void recalcularTotales(Carrito carrito) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (ItemCarrito item : carrito.getItems()) {
            BigDecimal cantidad = BigDecimal.valueOf(item.getCantidad());
            BigDecimal precio = item.getProducto().getPrecio();
            int descuento = item.getProducto().getDescuento() == null ? 0 : item.getProducto().getDescuento();

            BigDecimal precioConDescuento = precio
                    .multiply(BigDecimal.valueOf(100 - descuento))
                    .divide(BigDecimal.valueOf(100));

            subtotal = subtotal.add(precio.multiply(cantidad));
            total = total.add(precioConDescuento.multiply(cantidad));
        }

        carrito.setSubtotal(subtotal);
        carrito.setTotal(total);
    }
}
