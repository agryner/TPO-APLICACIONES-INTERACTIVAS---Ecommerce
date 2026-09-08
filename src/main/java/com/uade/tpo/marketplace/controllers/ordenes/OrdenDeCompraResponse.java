package com.uade.tpo.marketplace.controllers.ordenes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.uade.tpo.marketplace.entity.EstadoOrden;
import com.uade.tpo.marketplace.entity.OrdenDeCompra;

import lombok.Data;
import com.uade.tpo.marketplace.controllers.usuarios.UsuarioPublicoResponse;

/**
 * Vista publica de una orden de compra.
 *
 * Trae las dos puntas de la transaccion, comprador y vendedor, mas los
 * renglones con la copia de lo que se compro.
 *
 * Las dos puntas viajan como UsuarioPublicoResponse: solo el nombre y el nombre
 * de usuario. Ni el mail ni la direccion, aunque sea la contraparte de la
 * compra, porque la orden es el pago y no la entrega. Cuando exista la entidad
 * Envio, la direccion de destino va a vivir ahi, que ademas es su lugar: es un
 * dato de ese envio puntual y no el domicilio que el usuario tenga cargado en
 * la cuenta el dia que alguien mire la orden.
 */
@Data
public class OrdenDeCompraResponse {

    private Long id;
    private UsuarioPublicoResponse comprador;
    private UsuarioPublicoResponse vendedor;
    private BigDecimal subtotal;
    private BigDecimal total;
    private EstadoOrden estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaUltimoEstado;
    private List<OrderDetailResponse> items;

    /**
     * Pre : la entidad OrdenDeCompra, o null.
     * Post: la orden con comprador, vendedor, estado, totales y renglones.
     */
    public static OrdenDeCompraResponse from(OrdenDeCompra orden) {
        if (orden == null)
            return null;

        OrdenDeCompraResponse dto = new OrdenDeCompraResponse();
        dto.setId(orden.getId());
        dto.setComprador(UsuarioPublicoResponse.from(orden.getComprador()));
        dto.setVendedor(UsuarioPublicoResponse.from(orden.getVendedor()));
        dto.setSubtotal(orden.getSubtotal());
        dto.setTotal(orden.getTotal());
        dto.setEstado(orden.getEstado());
        dto.setFechaCreacion(orden.getFechaCreacion());
        dto.setFechaUltimoEstado(orden.getFechaUltimoEstado());
        dto.setItems(orden.getItems() == null ? List.of()
                : orden.getItems().stream().map(OrderDetailResponse::from).toList());
        return dto;
    }
}
