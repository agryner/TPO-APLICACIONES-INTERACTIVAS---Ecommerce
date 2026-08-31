package com.uade.tpo.marketplace.service;

import java.util.List;
import java.util.Optional;

import com.uade.tpo.marketplace.entity.Foto;
import com.uade.tpo.marketplace.controllers.FotoUploadRequest;
import com.uade.tpo.marketplace.exceptions.ArchivoInvalidoException;
import com.uade.tpo.marketplace.exceptions.FotoNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;

/**
 * Contrato de la logica de fotos.
 *
 * Lo consume FotosController y lo implementa FotoServiceImpl.
 */
public interface FotoService {

    List<Foto> getFotosByProducto(Long idProducto);

    Optional<Foto> getFotoById(Long idFoto);

    /** Guarda el archivo subido y lo asocia al producto indicado. */
    Foto subirFoto(FotoUploadRequest request)
            throws ProductoNoEncontradoException, ArchivoInvalidoException;

    /** Devuelve los bytes crudos de una foto. */
    byte[] getContenidoById(Long idFoto) throws FotoNoEncontradaException;

    void deleteFoto(Long idFoto) throws FotoNoEncontradaException;
}
