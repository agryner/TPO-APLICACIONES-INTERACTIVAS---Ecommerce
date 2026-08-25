package com.uade.tpo.marketplace.entity.dto;

import lombok.Data;

@Data
public class ItemCarritoRequest {
    private Long idProducto;
    private Integer cantidad;
}
