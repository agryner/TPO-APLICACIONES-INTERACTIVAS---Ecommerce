package com.uade.tpo.marketplace.service;

import java.util.List;
import java.util.Optional;

import com.uade.tpo.marketplace.entity.Foto;
import com.uade.tpo.marketplace.entity.dto.FotoRequest;
import com.uade.tpo.marketplace.exceptions.FotoNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;

public interface FotoService {

    List<Foto> getFotosByProducto(Long idProducto);

    Optional<Foto> getFotoById(Long idFoto);

    Foto createFoto(FotoRequest request) throws ProductoNoEncontradoException;

    void deleteFoto(Long idFoto) throws FotoNoEncontradaException;
}
