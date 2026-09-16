package com.uade.tpo.marketplace.controllers.ofertas;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.uade.tpo.marketplace.controllers.usuarios.UsuarioPublicoResponse;
import com.uade.tpo.marketplace.entity.EstadoOferta;
import com.uade.tpo.marketplace.entity.Oferta;

import lombok.Data;

@Data
public class OfertaResponse {
    private Long id;
    private Long idProducto;
    private String nombreProducto;
    private BigDecimal precioDeLista;
    private BigDecimal precioOfrecido;
    private Integer cantidad;
    private BigDecimal total;
    private UsuarioPublicoResponse comprador;
    private UsuarioPublicoResponse vendedor;
    private EstadoOferta estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaVencimiento;
    private LocalDateTime fechaRespuesta;

    /**
     * Pre : la entidad Oferta, o null.
     * Post: la oferta con las dos puntas y el precio de lista al lado del
     *       ofrecido, para que el vendedor vea de cuanto es la rebaja sin
     *       tener que ir a buscar el producto.
     */
    public static OfertaResponse from(Oferta oferta) {
        if (oferta == null)
            return null;

        OfertaResponse dto = new OfertaResponse();
        dto.setId(oferta.getId());
        dto.setCantidad(oferta.getCantidad());
        dto.setPrecioOfrecido(oferta.getPrecioOfrecido());
        dto.setTotal(oferta.getPrecioOfrecido()
                .multiply(BigDecimal.valueOf(oferta.getCantidad())));
        dto.setComprador(UsuarioPublicoResponse.from(oferta.getComprador()));
        dto.setEstado(oferta.getEstado());
        dto.setFechaCreacion(oferta.getFechaCreacion());
        dto.setFechaVencimiento(oferta.getFechaVencimiento());
        dto.setFechaRespuesta(oferta.getFechaRespuesta());

        if (oferta.getProducto() != null) {
            dto.setIdProducto(oferta.getProducto().getId());
            dto.setNombreProducto(oferta.getProducto().getNombre());
            dto.setPrecioDeLista(oferta.getProducto().getPrecio());
            dto.setVendedor(UsuarioPublicoResponse.from(oferta.getProducto().getVendedor()));
        }
        return dto;
    }
}
