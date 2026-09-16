package com.uade.tpo.marketplace.controllers.resenas;

import java.time.LocalDateTime;

import com.uade.tpo.marketplace.controllers.usuarios.UsuarioPublicoResponse;
import com.uade.tpo.marketplace.entity.Resena;

import lombok.Data;

@Data
public class ResenaResponse {
    private Long id;
    private Long idProducto;
    private String nombreProducto;
    private UsuarioPublicoResponse autor;
    private Integer puntaje;
    private String comentario;
    private LocalDateTime fecha;

    /**
     * Pre : la entidad Resena, o null.
     * Post: la resena con su autor en vista reducida. No lleva la orden: de que
     *       compra salio es asunto del comprador y del vendedor, no de quien
     *       esta mirando el producto.
     */
    public static ResenaResponse from(Resena resena) {
        if (resena == null)
            return null;

        ResenaResponse dto = new ResenaResponse();
        dto.setId(resena.getId());
        dto.setAutor(UsuarioPublicoResponse.from(resena.getAutor()));
        dto.setPuntaje(resena.getPuntaje());
        dto.setComentario(resena.getComentario());
        dto.setFecha(resena.getFecha());

        if (resena.getProducto() != null) {
            dto.setIdProducto(resena.getProducto().getId());
            dto.setNombreProducto(resena.getProducto().getNombre());
        }
        return dto;
    }
}
