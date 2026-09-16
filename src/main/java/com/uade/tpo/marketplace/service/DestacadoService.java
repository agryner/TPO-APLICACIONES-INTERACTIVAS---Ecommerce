package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.destacados.DestacadoResponse;
import com.uade.tpo.marketplace.controllers.productos.ProductoResponse;
import com.uade.tpo.marketplace.entity.NivelDestacado;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import java.util.List;

public interface DestacadoService {
    ProductoResponse destacar(Long idProducto, NivelDestacado nivel, Integer meses, Long idSolicitante)
            throws ProductoNoEncontradoException, UsuarioNoEncontradoException, AccesoDenegadoException;

    List<DestacadoResponse> getHistorial(Long idSolicitante)
            throws UsuarioNoEncontradoException, SinResultadosException;

    int bajarLosVencidos();
}
