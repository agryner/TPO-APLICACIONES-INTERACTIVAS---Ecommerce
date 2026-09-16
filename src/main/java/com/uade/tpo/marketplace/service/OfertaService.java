package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.ofertas.OfertaRequest;
import com.uade.tpo.marketplace.controllers.ofertas.OfertaResponse;
import com.uade.tpo.marketplace.exceptions.CompraPropiaException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.OfertaDuplicadaException;
import com.uade.tpo.marketplace.exceptions.OfertaNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.OfertaYaRespondidaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.PrecioOfrecidoInvalidoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoAceptaOfertasException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.RolNoComerciaException;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;
import com.uade.tpo.marketplace.exceptions.StockInsuficienteException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import java.util.List;

public interface OfertaService {
    OfertaResponse crear(OfertaRequest request, Long idSolicitante)
            throws ProductoNoEncontradoException, CompraPropiaException, PrecioOfrecidoInvalidoException, OfertaDuplicadaException, ProductoNoAceptaOfertasException, UsuarioNoEncontradoException, CuentaInactivaException, RolNoComerciaException;

    List<OfertaResponse> getMias(Long idSolicitante) throws SinResultadosException;

    OfertaResponse aceptar(Long idOferta, Long idSolicitante)
            throws OfertaNoEncontradaException, OperacionAjenaException, OfertaYaRespondidaException, StockInsuficienteException;

    OfertaResponse rechazar(Long idOferta, Long idSolicitante)
            throws OfertaNoEncontradaException, OperacionAjenaException, OfertaYaRespondidaException;

    OfertaResponse cancelar(Long idOferta, Long idSolicitante)
            throws OfertaNoEncontradaException, OperacionAjenaException, OfertaYaRespondidaException;

    int vencerLasViejas();
}
