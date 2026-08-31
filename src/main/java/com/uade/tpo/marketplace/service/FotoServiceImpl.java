package com.uade.tpo.marketplace.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.uade.tpo.marketplace.entity.Foto;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.controllers.FotoUploadRequest;
import com.uade.tpo.marketplace.exceptions.ArchivoInvalidoException;
import com.uade.tpo.marketplace.exceptions.FotoNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.repository.FotoRepository;
import com.uade.tpo.marketplace.repository.ProductoRepository;

import lombok.RequiredArgsConstructor;

/**
 * Logica de fotos: valida el archivo subido y lo guarda.
 *
 * Lo llama FotosController y usa FotoRepository para persistir y
 * ProductoRepository para resolver a que producto pertenece la imagen.
 * Rechaza lo que no sea una imagen antes de tocar la base.
 */
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

    @Transactional
    public Foto subirFoto(FotoUploadRequest request)
            throws ProductoNoEncontradoException, ArchivoInvalidoException {

        if (request.getIdProducto() == null)
            throw new ArchivoInvalidoException("Falta indicar el idProducto");

        MultipartFile file = request.getFile();
        if (file == null || file.isEmpty())
            throw new ArchivoInvalidoException("No se recibio ningun archivo en el campo 'file'");

        String tipoContenido = file.getContentType();
        if (tipoContenido == null || !tipoContenido.startsWith("image/"))
            throw new ArchivoInvalidoException("El archivo debe ser una imagen, se recibio: " + tipoContenido);

        Producto producto = productoRepository.findById(request.getIdProducto())
                .orElseThrow(ProductoNoEncontradoException::new);

        Foto foto = new Foto();
        foto.setProducto(producto);
        foto.setTipoContenido(tipoContenido);
        foto.setTamanio(file.getSize());
        foto.setNombreArchivo(file.getOriginalFilename());
        try {
            foto.setContenido(file.getBytes());
        } catch (IOException e) {
            throw new ArchivoInvalidoException("No se pudo leer el archivo: " + e.getMessage());
        }

        return fotoRepository.save(foto);
    }

    public byte[] getContenidoById(Long idFoto) throws FotoNoEncontradaException {
        return fotoRepository.findById(idFoto)
                .orElseThrow(FotoNoEncontradaException::new)
                .getContenido();
    }

    @Transactional
    public void deleteFoto(Long idFoto) throws FotoNoEncontradaException {
        Foto foto = fotoRepository.findById(idFoto)
                .orElseThrow(FotoNoEncontradaException::new);

        // Producto mapea sus fotos con cascade ALL y las carga en EAGER, asi que
        // al leer la foto tambien queda gestionada la coleccion que la contiene.
        // Si no la sacamos de ahi, el cascade PERSIST la revive en el flush y el
        // borrado se pierde sin error. Sacandola, orphanRemoval dispara el delete.
        Producto producto = foto.getProducto();
        if (producto != null && producto.getFotos() != null)
            producto.getFotos().remove(foto);

        fotoRepository.delete(foto);
    }
}
