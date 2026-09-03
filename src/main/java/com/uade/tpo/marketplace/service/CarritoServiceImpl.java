package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.entity.dto.CarritoResponse;
import com.uade.tpo.marketplace.entity.dto.ItemCarritoRequest;
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
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.CompraPropiaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.repository.CarritoRepository;
import com.uade.tpo.marketplace.repository.ItemCarritoRepository;
import com.uade.tpo.marketplace.repository.ProductoRepository;
import com.uade.tpo.marketplace.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Logica del carrito: items, totales y vencimiento.
 *
 * Lo llama CarritosController y usa CarritoRepository, ProductoRepository y
 * UsuarioRepository. Los items no tienen repositorio propio: se manejan a
 * traves de la coleccion del Carrito, que los persiste en cascada. Antes de
 * cada operacion vacia el carrito si paso su fechaLimite, y despues recalcula
 * subtotal y total.
 */
@Service
@RequiredArgsConstructor
public class CarritoServiceImpl implements CarritoService {

    private final CarritoRepository carritoRepository;
    private final ProductoRepository productoRepository;
    private final ItemCarritoRepository itemCarritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AutorizacionService autorizacion;

    /** Cuanto vive el carrito desde la ultima vez que se modifico. */
    @Value("${marketplace.carrito.minutos-vigencia:1440}")
    private long minutosVigencia;

    @Transactional
    public CarritoResponse obtenerCarrito(Long idUsuario, Long idSolicitante)
            throws OperacionAjenaException, UsuarioNoEncontradoException {
        autorizacion.validarDuenio(idSolicitante, idUsuario);
        return CarritoResponse.from(obtenerCarritoEntidad(idUsuario));
    }

    /**
     * Version para uso entre services: devuelve la entidad, no el DTO.
     *
     * OrdenDeCompraServiceImpl necesita recorrer los items y descontar stock,
     * asi que no le alcanza con la vista de solo lectura.
     */
    @Transactional
    public Carrito obtenerCarritoEntidad(Long idUsuario) throws UsuarioNoEncontradoException {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);

        Carrito carrito = carritoRepository.findByUsuarioId(idUsuario)
                .orElseGet(() -> crearCarritoVacio(usuario));

        return vaciarSiVencio(carrito);
    }

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

    @Transactional
    public CarritoResponse agregarItem(Long idUsuario, ItemCarritoRequest request,
            Long idSolicitante)
            throws OperacionAjenaException, UsuarioNoEncontradoException,
            ProductoNoEncontradoException, StockInsuficienteException,
            CompraPropiaException {
        autorizacion.validarDuenio(idSolicitante, idUsuario);

        Carrito carrito = obtenerCarritoEntidad(idUsuario);
        // Un producto dado de baja o fuera del catalogo no existe para quien
        // compra, asi que se trata igual que uno inexistente.
        Producto producto = productoRepository.findById(request.getIdProducto())
                .filter(Producto::getActivo)
                .filter(p -> p.getEstadoPublicacion() == EstadoPublicacion.PUBLICADO)
                .orElseThrow(ProductoNoEncontradoException::new);

        // Cortar aca y no en el checkout: el comprador se entera al tocar el
        // boton de agregar y no despues de armar todo el carrito.
        if (producto.getVendedor().getId().equals(idUsuario))
            throw new CompraPropiaException(producto);

        int cantidad = request.getCantidad() == null ? 1 : request.getCantidad();
        if (producto.getStock() < cantidad)
            throw new StockInsuficienteException(producto, cantidad);

        // Si el producto ya estaba en el carrito, se acumula la cantidad.
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
            // Lo que ya habia en el carrito mas lo que se suma ahora.
            if (producto.getStock() < item.getCantidad() + cantidad)
                throw new StockInsuficienteException(producto, item.getCantidad() + cantidad);
            item.setCantidad(item.getCantidad() + cantidad);
        }

        renovarVigencia(carrito);
        recalcularTotales(carrito);
        return CarritoResponse.from(carritoRepository.save(carrito));
    }

    @Transactional
    public CarritoResponse modificarCantidad(Long idUsuario, Long idItem, Integer nuevaCantidad,
            Long idSolicitante)
            throws OperacionAjenaException, UsuarioNoEncontradoException,
            ItemCarritoNoEncontradoException, StockInsuficienteException {
        autorizacion.validarDuenio(idSolicitante, idUsuario);

        // Pedir cero o menos es sacarlo del carrito, no dejar un item vacio.
        if (nuevaCantidad == null || nuevaCantidad <= 0)
            return eliminarItem(idUsuario, idItem, idSolicitante);

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

    @Transactional
    public CarritoResponse eliminarItem(Long idUsuario, Long idItem, Long idSolicitante)
            throws OperacionAjenaException, UsuarioNoEncontradoException,
            ItemCarritoNoEncontradoException {
        autorizacion.validarDuenio(idSolicitante, idUsuario);

        Carrito carrito = obtenerCarritoEntidad(idUsuario);

        boolean removido = carrito.getItems().removeIf(i -> i.getId().equals(idItem));
        if (!removido)
            throw new ItemCarritoNoEncontradoException();

        renovarVigencia(carrito);
        recalcularTotales(carrito);
        return CarritoResponse.from(carritoRepository.save(carrito));
    }

    @Transactional
    public CarritoResponse vaciar(Long idUsuario, Long idSolicitante)
            throws OperacionAjenaException, UsuarioNoEncontradoException {
        autorizacion.validarDuenio(idSolicitante, idUsuario);
        return CarritoResponse.from(vaciarCarrito(obtenerCarritoEntidad(idUsuario)));
    }

    private Carrito crearCarritoVacio(Usuario usuario) {
        Carrito carrito = new Carrito();
        carrito.setUsuario(usuario);
        carrito.setSubtotal(BigDecimal.ZERO);
        carrito.setTotal(BigDecimal.ZERO);
        return carritoRepository.save(carrito);
    }

    /**
     * El carrito no se borra: se vacia. El usuario conserva siempre el mismo.
     */
    private Carrito vaciarSiVencio(Carrito carrito) {
        boolean vencio = carrito.getFechaLimite() != null
                && carrito.getFechaLimite().isBefore(LocalDateTime.now());

        return vencio ? vaciarCarrito(carrito) : carrito;
    }

    private Carrito vaciarCarrito(Carrito carrito) {
        carrito.getItems().clear();
        carrito.setFechaLimite(null);
        carrito.setSubtotal(BigDecimal.ZERO);
        carrito.setTotal(BigDecimal.ZERO);
        return carritoRepository.save(carrito);
    }

    /**
     * Cada modificacion corre la fecha limite hacia adelante. Un carrito vacio
     * no vence porque no hay nada que vaciar.
     */
    private void renovarVigencia(Carrito carrito) {
        carrito.setFechaLimite(carrito.getItems().isEmpty()
                ? null
                : LocalDateTime.now().plusMinutes(minutosVigencia));
    }

    /**
     * subtotal: suma de precio de lista por cantidad.
     * total: lo mismo, pero aplicando el descuento de cada producto.
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
