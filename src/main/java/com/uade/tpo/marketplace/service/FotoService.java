package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.fotos.FotoResponse;
import com.uade.tpo.marketplace.entity.EstadoVerificacion;
import com.uade.tpo.marketplace.controllers.fotos.FotoUploadRequest;
import java.util.List;

import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.ArchivoInvalidoException;
import com.uade.tpo.marketplace.exceptions.FotoNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.FotoRechazadaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.AdminNoComerciaException;

/**
 * Contrato de la logica de fotos.
 *
 * Lo consume FotosController y lo implementa FotoServiceImpl.
 */
public interface FotoService {

    List<FotoResponse> getFotosByProducto(Long idProducto)
            throws ProductoNoEncontradoException;

    FotoResponse getFotoById(Long idFoto) throws FotoNoEncontradaException;

    FotoResponse subirFoto(FotoUploadRequest request, Long idSolicitante)
            throws ProductoNoEncontradoException, ArchivoInvalidoException,
            FotoRechazadaException, OperacionAjenaException, CuentaInactivaException, UsuarioNoEncontradoException, AdminNoComerciaException;

    byte[] getContenidoById(Long idFoto) throws FotoNoEncontradaException;

    List<FotoResponse> getPendientesDeRevision(Long idSolicitante, EstadoVerificacion estado)
            throws UsuarioNoEncontradoException, AccesoDenegadoException;

    FotoResponse revisarFoto(Long idFoto, boolean aprobada, Long idSolicitante)
            throws FotoNoEncontradaException, UsuarioNoEncontradoException,
            AccesoDenegadoException;

    void deleteFoto(Long idFoto, Long idSolicitante)
            throws FotoNoEncontradaException, OperacionAjenaException, CuentaInactivaException, UsuarioNoEncontradoException;
}
