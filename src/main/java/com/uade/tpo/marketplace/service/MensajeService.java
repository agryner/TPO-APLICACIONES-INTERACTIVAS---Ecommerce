package com.uade.tpo.marketplace.service;

import java.util.List;

import com.uade.tpo.marketplace.controllers.mensajes.MensajeResponse;
import com.uade.tpo.marketplace.exceptions.ChatNoDisponibleException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.OrdenNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;

public interface MensajeService {
    List<MensajeResponse> getMensajes(Long idOrden, Long idSolicitante)
            throws OrdenNoEncontradaException, OperacionAjenaException, SinResultadosException;

    MensajeResponse enviar(Long idOrden, Long idSolicitante, String texto)
            throws OrdenNoEncontradaException, OperacionAjenaException, ChatNoDisponibleException;
}
