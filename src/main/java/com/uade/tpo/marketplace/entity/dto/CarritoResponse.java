package com.uade.tpo.marketplace.entity.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.uade.tpo.marketplace.entity.Carrito;

import lombok.Data;

/**
 * Vista publica del carrito de un usuario.
 *
 * Del duenio expone solo el id, porque el carrito siempre se pide dentro de
 * /usuarios/{idUsuario}/carrito y el cliente ya sabe de quien es.
 */
@Data
public class CarritoResponse {

    private Long id;
    private Long idUsuario;
    private BigDecimal subtotal;
    private BigDecimal total;
    private LocalDateTime fechaLimite;
    private List<ItemCarritoResponse> items;

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
