package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.envios.EntregaResponse;
import com.uade.tpo.marketplace.controllers.envios.EnvioResponse;
import com.uade.tpo.marketplace.entity.EstadoEnvio;
import com.uade.tpo.marketplace.entity.OrdenDeCompra;
import com.uade.tpo.marketplace.exceptions.CambioDeEstadoNoPermitidoException;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.EnvioNoDisponibleException;
import com.uade.tpo.marketplace.exceptions.EnvioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;
import com.uade.tpo.marketplace.exceptions.TransicionInvalidaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import java.util.List;

public interface EnvioService {
    void crearParaOrden(OrdenDeCompra orden);

    List<EnvioResponse> getMios(Long idSolicitante)
            throws UsuarioNoEncontradoException, SinResultadosException;

    EnvioResponse getById(Long idEnvio, Long idSolicitante)
            throws EnvioNoEncontradoException, OperacionAjenaException, UsuarioNoEncontradoException;

    List<EntregaResponse> getHistorial(Long idSolicitante)
            throws UsuarioNoEncontradoException, AccesoDenegadoException,
            SinResultadosException;

    EnvioResponse recibir(String numeroSeguimiento, Long idSolicitante)
            throws EnvioNoEncontradoException, EnvioNoDisponibleException,
            CambioDeEstadoNoPermitidoException, UsuarioNoEncontradoException;

    EnvioResponse actualizarEstado(Long idEnvio, EstadoEnvio estado, Long idSolicitante)
            throws EnvioNoEncontradoException, TransicionInvalidaException, CambioDeEstadoNoPermitidoException, UsuarioNoEncontradoException;
}
