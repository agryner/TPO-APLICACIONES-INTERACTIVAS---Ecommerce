package com.uade.tpo.marketplace.controllers.notificaciones;

import java.time.LocalDateTime;

import com.uade.tpo.marketplace.entity.Notificacion;
import com.uade.tpo.marketplace.entity.TipoNotificacion;

import lombok.Data;

@Data
public class NotificacionResponse {
    private Long id;
    private TipoNotificacion tipo;
    private String mensaje;
    private String link;
    private Boolean leida;
    private LocalDateTime fecha;

    /**
     * Pre : la entidad Notificacion, o null.
     * Post: el aviso sin el destinatario: quien lo pide ya sabe que son suyas.
     *       El tipo viaja aunque el mensaje ya lo diga, para que el front
     *       pueda ponerle un icono distinto a cada uno.
     */
    public static NotificacionResponse from(Notificacion notificacion) {
        if (notificacion == null)
            return null;

        NotificacionResponse dto = new NotificacionResponse();
        dto.setId(notificacion.getId());
        dto.setTipo(notificacion.getTipo());
        dto.setMensaje(notificacion.getMensaje());
        dto.setLink(notificacion.getLink());
        dto.setLeida(notificacion.getLeida());
        dto.setFecha(notificacion.getFecha());
        return dto;
    }
}
