package com.uade.tpo.marketplace.controllers.destacados;

import java.time.LocalDateTime;

import com.uade.tpo.marketplace.controllers.usuarios.UsuarioPublicoResponse;
import com.uade.tpo.marketplace.entity.Destacado;
import com.uade.tpo.marketplace.entity.NivelDestacado;

import lombok.Data;

@Data
public class DestacadoResponse {
    private Long id;
    private Long idProducto;
    private String nombreProducto;
    private UsuarioPublicoResponse vendedor;
    private NivelDestacado nivel;
    private Integer meses;
    private LocalDateTime desde;
    private LocalDateTime hasta;
    private Boolean vigente;

    /**
     * Pre : la entidad Destacado, o null.
     * Post: una fila del historial. vigente se calcula contra la fecha de hoy:
     *       el historial guarda lo que se vendio, no lo que esta corriendo.
     */
    public static DestacadoResponse from(Destacado destacado) {
        if (destacado == null)
            return null;

        DestacadoResponse dto = new DestacadoResponse();
        dto.setId(destacado.getId());
        dto.setNivel(destacado.getNivel());
        dto.setMeses(destacado.getMeses());
        dto.setDesde(destacado.getDesde());
        dto.setHasta(destacado.getHasta());
        dto.setVigente(destacado.getHasta() != null
                && destacado.getHasta().isAfter(LocalDateTime.now()));

        if (destacado.getProducto() != null) {
            dto.setIdProducto(destacado.getProducto().getId());
            dto.setNombreProducto(destacado.getProducto().getNombre());
            dto.setVendedor(UsuarioPublicoResponse.from(
                    destacado.getProducto().getVendedor()));
        }
        return dto;
    }
}
