package com.uade.tpo.marketplace.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.uade.tpo.marketplace.entity.Foto;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.entity.dto.FotoRequest;
import com.uade.tpo.marketplace.exceptions.FotoNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.repository.FotoRepository;
import com.uade.tpo.marketplace.repository.ProductoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FotoServiceImpl implements FotoService {

    private final FotoRepository fotoRepository;
    private final ProductoRepository productoRepository;

    public List<Foto> getFotosByProducto(Long idProducto) {
        return fotoRepository.findByProductoId(idProducto);
    }

    public Optional<Foto> getFotoById(Long idFoto) {
        return fotoRepository.findById(idFoto);
    }

    public Foto createFoto(FotoRequest request) throws ProductoNoEncontradoException {
        Producto producto = productoRepository.findById(request.getIdProducto())
                .orElseThrow(ProductoNoEncontradoException::new);

        Foto foto = new Foto();
        foto.setUrl(request.getUrl());
        foto.setProducto(producto);
        return fotoRepository.save(foto);
    }

    public void deleteFoto(Long idFoto) throws FotoNoEncontradaException {
        if (!fotoRepository.existsById(idFoto))
            throw new FotoNoEncontradaException();

        fotoRepository.deleteById(idFoto);
    }
}
