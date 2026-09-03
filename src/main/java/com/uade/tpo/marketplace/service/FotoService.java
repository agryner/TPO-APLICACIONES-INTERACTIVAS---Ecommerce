package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.entity.dto.FotoResponse;
import com.uade.tpo.marketplace.entity.EstadoVerificacion;
import com.uade.tpo.marketplace.entity.dto.FotoUploadRequest;
import java.util.List;

import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.ArchivoInvalidoException;
import com.uade.tpo.marketplace.exceptions.FotoNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.FotoRechazadaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;

/**
 * Contrato de la logica de fotos.
 *
 * Lo consume FotosController y lo implementa FotoServiceImpl.
 */
public interface FotoService {

    /**
     * Fotos de un producto.
     *
     * Tira 404 si el producto no existe, para distinguirlo de un producto real
     * que todavia no tiene fotos: los dos casos devolverian una lista vacia. Un
     * producto en BORRADOR es justamente eso, asi que la lista vacia es una
     * respuesta legitima y no un error.
     */
    List<FotoResponse> getFotosByProducto(Long idProducto)
            throws ProductoNoEncontradoException;

    FotoResponse getFotoById(Long idFoto) throws FotoNoEncontradaException;

    /** Guarda el archivo subido y lo asocia al producto indicado. */
    /** Solo el vendedor duenio del producto puede subirle fotos. */
    FotoResponse subirFoto(FotoUploadRequest request, Long idSolicitante)
            throws ProductoNoEncontradoException, ArchivoInvalidoException,
            FotoRechazadaException, OperacionAjenaException, CuentaInactivaException, UsuarioNoEncontradoException;

    /** Devuelve los bytes crudos de una foto. */
    byte[] getContenidoById(Long idFoto) throws FotoNoEncontradaException;

    /** Fotos que quedaron esperando que un admin las mire. Solo para ADMIN. */
    List<FotoResponse> getPendientesDeRevision(Long idSolicitante, EstadoVerificacion estado)
            throws UsuarioNoEncontradoException, AccesoDenegadoException;

    /**
     * Resuelve una foto en revision. Aprobarla la deja visible; rechazarla la
     * borra, porque una foto que no corresponde no tiene por que quedar.
     */
    FotoResponse revisarFoto(Long idFoto, boolean aprobada, Long idSolicitante)
            throws FotoNoEncontradaException, UsuarioNoEncontradoException,
            AccesoDenegadoException;

    /** Solo el vendedor duenio del producto puede borrarle fotos. */
    void deleteFoto(Long idFoto, Long idSolicitante)
            throws FotoNoEncontradaException, OperacionAjenaException, CuentaInactivaException, UsuarioNoEncontradoException;
}
