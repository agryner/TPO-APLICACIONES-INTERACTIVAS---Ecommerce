package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.notificaciones.NotificacionResponse;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.entity.TipoNotificacion;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.NotificacionNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;
import java.util.List;

public interface NotificacionService {
    void crear(Usuario destinatario, TipoNotificacion tipo, String mensaje, String link);

    void avisarAQuienesLoTienenGuardado(Producto producto, TipoNotificacion tipo, String mensaje);

    List<NotificacionResponse> getMias(Long idUsuario) throws SinResultadosException;

    long contarNoLeidas(Long idUsuario);

    NotificacionResponse marcarLeida(Long idNotificacion, Long idUsuario)
            throws NotificacionNoEncontradaException, OperacionAjenaException;
}
