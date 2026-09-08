package com.uade.tpo.marketplace.controllers.ordenes;

import java.math.BigDecimal;

import com.uade.tpo.marketplace.entity.OrderDetail;

import lombok.Data;

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

    /**
     * Pre : la entidad OrderDetail, o null.
     * Post: el renglon con el nombre y el precio que tenia el producto al
     *       momento de la compra, no los actuales.
     */
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
