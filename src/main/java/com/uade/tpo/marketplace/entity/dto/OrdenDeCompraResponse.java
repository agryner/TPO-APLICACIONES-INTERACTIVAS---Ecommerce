package com.uade.tpo.marketplace.entity.dto;

import java.math.BigDecimal;
import java.util.List;

import com.uade.tpo.marketplace.entity.EstadoOrden;
import com.uade.tpo.marketplace.entity.OrdenDeCompra;

import lombok.Data;

/**
 * Vista publica de una orden de compra.
 *
 * Trae las dos puntas de la transaccion, comprador y vendedor, mas los
 * renglones con la copia de lo que se compro.
 */
@Data
public class OrdenDeCompraResponse {

    private Long id;
    private UsuarioResponse comprador;
    private UsuarioResponse vendedor;
    private BigDecimal subtotal;
    private BigDecimal total;
    private EstadoOrden estado;
    private List<OrderDetailResponse> items;

    public static OrdenDeCompraResponse from(OrdenDeCompra orden) {
        if (orden == null)
            return null;

        OrdenDeCompraResponse dto = new OrdenDeCompraResponse();
        dto.setId(orden.getId());
        dto.setComprador(UsuarioResponse.from(orden.getComprador()));
        dto.setVendedor(UsuarioResponse.from(orden.getVendedor()));
        dto.setSubtotal(orden.getSubtotal());
        dto.setTotal(orden.getTotal());
        dto.setEstado(orden.getEstado());
        dto.setItems(orden.getItems() == null ? List.of()
                : orden.getItems().stream().map(OrderDetailResponse::from).toList());
        return dto;
    }
}
