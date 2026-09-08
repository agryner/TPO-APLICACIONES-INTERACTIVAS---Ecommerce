package com.uade.tpo.marketplace.controllers.carritos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.uade.tpo.marketplace.entity.Carrito;

import lombok.Data;

@Data
public class CarritoResponse {
    private Long id;
    private Long idUsuario;
    private BigDecimal subtotal;
    private BigDecimal total;
    private LocalDateTime fechaLimite;
    private List<ItemCarritoResponse> items;

    /**
     * Pre : la entidad Carrito, o null.
     * Post: el DTO con los items ordenados, el subtotal, el total y la fecha
     *       de vencimiento. Devuelve null si entra null.
     */
    public static CarritoResponse from(Carrito carrito) {
        if (carrito == null)
            return null;

        CarritoResponse dto = new CarritoResponse();
        dto.setId(carrito.getId());
        dto.setIdUsuario(carrito.getUsuario() == null ? null : carrito.getUsuario().getId());
        dto.setSubtotal(carrito.getSubtotal());
        dto.setTotal(carrito.getTotal());
        dto.setFechaLimite(carrito.getFechaLimite());
        dto.setItems(carrito.getItems() == null ? List.of()
                : carrito.getItems().stream().map(ItemCarritoResponse::from).toList());
        return dto;
    }
}
