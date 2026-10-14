package com.uade.tpo.marketplace.controllers.mensajes;

import java.time.LocalDateTime;

import com.uade.tpo.marketplace.entity.Mensaje;

import lombok.Data;

@Data
public class MensajeResponse {
    private Long id;
    private Long idAutor;
    private String nombreUsuarioAutor;
    private String texto;
    private LocalDateTime fecha;

    /**
     * Pre : la entidad Mensaje, o null.
     * Post: el mensaje con el id y el usuario de quien lo escribio, para que el
     *       front distinga los propios de los de la otra parte.
     */
    public static MensajeResponse from(Mensaje mensaje) {
        if (mensaje == null)
            return null;

        MensajeResponse dto = new MensajeResponse();
        dto.setId(mensaje.getId());
        dto.setIdAutor(mensaje.getAutor().getId());
        dto.setNombreUsuarioAutor(mensaje.getAutor().getNombreUsuario());
        dto.setTexto(mensaje.getTexto());
        dto.setFecha(mensaje.getFecha());
        return dto;
    }
}
