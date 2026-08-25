package com.uade.tpo.marketplace.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.marketplace.entity.Carrito;
import com.uade.tpo.marketplace.entity.ItemCarrito;
import com.uade.tpo.marketplace.entity.OrderDetail;
import com.uade.tpo.marketplace.entity.OrdenDeCompra;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.entity.dto.OrdenDeCompraRequest;
import com.uade.tpo.marketplace.exceptions.CarritoVacioException;
import com.uade.tpo.marketplace.exceptions.OrdenNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.StockInsuficienteException;
import com.uade.tpo.marketplace.repository.OrdenDeCompraRepository;
import com.uade.tpo.marketplace.repository.ProductoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrdenDeCompraServiceImpl implements OrdenDeCompraService {

    private static final String ESTADO_INICIAL = "PENDIENTE";

    private final OrdenDeCompraRepository ordenRepository;
    private final ProductoRepository productoRepository;
    private final CarritoService carritoService;

    public List<OrdenDeCompra> getOrdenes() {
        return ordenRepository.findAll();
    }

    public List<OrdenDeCompra> getOrdenesByUsuario(Long idUsuario) {
        return ordenRepository.findByUsuarioId(idUsuario);
    }

    public Optional<OrdenDeCompra> getOrdenById(Long idOrden) {
        return ordenRepository.findById(idOrden);
    }

    /**
     * Confirma un carrito: valida el stock de cada item, lo descuenta y
     * congela los totales del carrito en la orden.
     */
    @Transactional
    public OrdenDeCompra createOrden(OrdenDeCompraRequest request)
            throws UsuarioNoEncontradoException, CarritoVacioException, StockInsuficienteException {
        Carrito carrito = carritoService.obtenerCarrito(request.getIdUsuario());

        if (carrito.getItems().isEmpty())
            throw new CarritoVacioException();

        for (ItemCarrito item : carrito.getItems()) {
            Producto producto = item.getProducto();
            if (producto.getStock() < item.getCantidad())
                throw new StockInsuficienteException();
        }

        OrdenDeCompra orden = new OrdenDeCompra();
        orden.setCarrito(carrito);
        orden.setUsuario(carrito.getUsuario());
        orden.setEstado(ESTADO_INICIAL);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (ItemCarrito item : carrito.getItems()) {
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
        orden = ordenRepository.save(orden);

        // El carrito es unico por usuario y se reutiliza: se vacia tras la compra.
        carritoService.vaciar(request.getIdUsuario());
        return orden;
    }

    public OrdenDeCompra actualizarEstado(Long idOrden, String estado) throws OrdenNoEncontradaException {
        OrdenDeCompra orden = ordenRepository.findById(idOrden)
                .orElseThrow(OrdenNoEncontradaException::new);

        orden.setEstado(estado);
        return ordenRepository.save(orden);
    }
}
