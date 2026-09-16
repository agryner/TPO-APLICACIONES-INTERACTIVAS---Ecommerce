package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.resenas.CalificacionResponse;
import com.uade.tpo.marketplace.controllers.resenas.ResenaRequest;
import com.uade.tpo.marketplace.controllers.resenas.ResenaResponse;
import com.uade.tpo.marketplace.exceptions.EntregaPendienteException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.OrdenNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.ProductoFueraDeLaOrdenException;
import com.uade.tpo.marketplace.exceptions.ResenaDuplicadaException;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import java.util.List;

public interface ResenaService {
    ResenaResponse crear(ResenaRequest request, Long idSolicitante)
            throws OrdenNoEncontradaException, OperacionAjenaException, ProductoFueraDeLaOrdenException, EntregaPendienteException, ResenaDuplicadaException, UsuarioNoEncontradoException;

    List<ResenaResponse> getDeProducto(Long idProducto) throws SinResultadosException;

    CalificacionResponse getCalificacion(String nombreUsuario)
            throws UsuarioNoEncontradoException;
}
