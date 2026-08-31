package com.uade.tpo.marketplace.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.marketplace.entity.Carrito;
import com.uade.tpo.marketplace.entity.ItemCarrito;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.controllers.ItemCarritoRequest;
import com.uade.tpo.marketplace.exceptions.ItemCarritoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.StockInsuficienteException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.repository.CarritoRepository;
import com.uade.tpo.marketplace.repository.ProductoRepository;
import com.uade.tpo.marketplace.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Logica del carrito: items, totales y vencimiento.
 *
 * Lo llama CarritosController y usa CarritoRepository, ItemCarritoRepository,
 * ProductoRepository y UsuarioRepository. Antes de cada operacion vacia el
 * carrito si paso su fechaLimite, y despues recalcula subtotal y total.
 */
@Service
@RequiredArgsConstructor
public class CarritoServiceImpl implements CarritoService {

    private final CarritoRepository carritoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    /** Cuanto vive el carrito desde la ultima vez que se modifico. */
    @Value("${marketplace.carrito.minutos-vigencia:1440}")
    private long minutosVigencia;

    @Transactional
    public Carrito obtenerCarrito(Long idUsuario) throws UsuarioNoEncontradoException {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);

        Carrito carrito = carritoRepository.findByUsuarioId(idUsuario)
                .orElseGet(() -> crearCarritoVacio(usuario));

        return vaciarSiVencio(carrito);
    }

    @Transactional
    public Carrito agregarItem(Long idUsuario, ItemCarritoRequest request)
            throws UsuarioNoEncontradoException, ProductoNoEncontradoException, StockInsuficienteException {
        Carrito carrito = obtenerCarrito(idUsuario);
        Producto producto = productoRepository.findById(request.getIdProducto())
                .orElseThrow(ProductoNoEncontradoException::new);

        int cantidad = request.getCantidad() == null ? 1 : request.getCantidad();
        if (producto.getStock() < cantidad)
            throw new StockInsuficienteException();

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
            if (producto.getStock() < item.getCantidad() + cantidad)
                throw new StockInsuficienteException();
            item.setCantidad(item.getCantidad() + cantidad);
        }

        renovarVigencia(carrito);
        recalcularTotales(carrito);
        return carritoRepository.save(carrito);
    }

    @Transactional
    public Carrito eliminarItem(Long idUsuario, Long idItem)
            throws UsuarioNoEncontradoException, ItemCarritoNoEncontradoException {
        Carrito carrito = obtenerCarrito(idUsuario);

        boolean removido = carrito.getItems().removeIf(i -> i.getId().equals(idItem));
        if (!removido)
            throw new ItemCarritoNoEncontradoException();

        renovarVigencia(carrito);
        recalcularTotales(carrito);
        return carritoRepository.save(carrito);
    }

    @Transactional
    public Carrito vaciar(Long idUsuario) throws UsuarioNoEncontradoException {
        Carrito carrito = obtenerCarrito(idUsuario);
        return vaciarCarrito(carrito);
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
