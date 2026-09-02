package com.uade.tpo.marketplace.entity.dto;

import java.math.BigDecimal;

import com.uade.tpo.marketplace.entity.OrderDetail;

import lombok.Data;

/**
 * Renglon de una orden ya cerrada.
 *
 * Expone los importes calculados de la entidad (precio final y total) para que
 * el cliente no tenga que rehacer la cuenta del descuento por su lado.
 */
@Data
public class OrderDetailResponse {

    private Long id;
    private Long idProducto;
    private String nombre;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private Integer descuento;
    private BigDecimal precioFinal;
    private BigDecimal subtotal;
    private BigDecimal total;

    public static OrderDetailResponse from(OrderDetail detalle) {
        if (detalle == null)
            return null;

        OrderDetailResponse dto = new OrderDetailResponse();
        dto.setId(detalle.getId());
        dto.setIdProducto(detalle.getProducto() == null ? null : detalle.getProducto().getId());
        dto.setNombre(detalle.getNombre());
        dto.setCantidad(detalle.getCantidad());
        dto.setPrecioUnitario(detalle.getPrecioUnitario());
        dto.setDescuento(detalle.getDescuento());
        dto.setPrecioFinal(detalle.getPrecioFinal());
        dto.setSubtotal(detalle.getSubtotal());
        dto.setTotal(detalle.getTotal());
        return dto;
    }
}
