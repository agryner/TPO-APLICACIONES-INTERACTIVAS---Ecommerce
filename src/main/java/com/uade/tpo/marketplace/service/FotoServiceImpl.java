package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.fotos.FotoResponse;
import com.uade.tpo.marketplace.controllers.fotos.FotoUploadRequest;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.uade.tpo.marketplace.entity.EstadoVerificacion;
import com.uade.tpo.marketplace.entity.EstadoPublicacion;
import com.uade.tpo.marketplace.entity.Foto;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.ArchivoInvalidoException;
import com.uade.tpo.marketplace.exceptions.AdminNoComerciaException;
import com.uade.tpo.marketplace.exceptions.FotoNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.FotoRechazadaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.repository.FotoRepository;
import com.uade.tpo.marketplace.repository.ProductoRepository;

import lombok.RequiredArgsConstructor;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;

@Service
@RequiredArgsConstructor
public class FotoServiceImpl implements FotoService {
    private static final Logger log = LoggerFactory.getLogger(FotoServiceImpl.class);

    @Value("${marketplace.ia.umbral-aprobacion:0.7}")
    private double umbralAprobacion;

    @Value("${marketplace.ia.umbral-rechazo:0.4}")
    private double umbralRechazo;

    private final FotoRepository fotoRepository;
    private final ProductoRepository productoRepository;
    private final VerificadorImagenService verificador;
    private final AutorizacionService autorizacion;
    private final CarritoService carritoService;

    /**
     * Pre : el id del producto.
     * Post: los metadatos de sus fotos, sin los bytes. Tira
     *       ProductoNoEncontradoException si el producto no existe, para
     *       distinguirlo de uno real que todavia no tiene fotos.
     */
    public List<FotoResponse> getFotosByProducto(Long idProducto)
            throws ProductoNoEncontradoException {
        if (!productoRepository.existsById(idProducto))
            throw new ProductoNoEncontradoException();

        return fotoRepository.findByProductoId(idProducto).stream()
                .map(FotoResponse::from)
                .toList();
    }

    public FotoResponse getFotoById(Long idFoto) throws FotoNoEncontradaException {
        return fotoRepository.findById(idFoto)
                .map(FotoResponse::from)
                .orElseThrow(FotoNoEncontradaException::new);
    }

    /**
     * Pre : el request con el archivo y el idProducto, mas el id de quien
     *       sube.
     * Post: la foto guardada. Si el producto estaba en BORRADOR queda
     *       PUBLICADO. Tira ArchivoInvalidoException si el contenido no es una
     *       imagen, FotoRechazadaException si la IA la descarta, y
     *       AdminNoComerciaException si quien sube es ADMIN.
     */
    @Transactional
    public FotoResponse subirFoto(FotoUploadRequest request, Long idSolicitante)
            throws ProductoNoEncontradoException, ArchivoInvalidoException,
            FotoRechazadaException, OperacionAjenaException, CuentaInactivaException, UsuarioNoEncontradoException, AdminNoComerciaException {
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

        autorizacion.validarDuenio(idSolicitante, producto.getVendedor().getId());

        autorizacion.validarQueNoSeaAdmin(idSolicitante);

        Foto foto = new Foto();
        foto.setProducto(producto);
        foto.setTipoContenido(tipoContenido);
        foto.setTamanio(file.getSize());
        foto.setNombreArchivo(file.getOriginalFilename());

        byte[] contenido;
        try {
            contenido = file.getBytes();
        } catch (IOException e) {
            throw new ArchivoInvalidoException("No se pudo leer el archivo: " + e.getMessage());
        }
        if (!pareceImagen(contenido))
            throw new ArchivoInvalidoException(
                    "El archivo no es una imagen: su contenido no corresponde a ningun formato conocido");

        foto.setContenido(contenido);

        verificar(foto, contenido, producto);
        foto = fotoRepository.save(foto);

        producto.setEstadoPublicacion(EstadoPublicacion.PUBLICADO);
        productoRepository.save(producto);

        return FotoResponse.from(foto);
    }

    private static final byte[][] FIRMAS = {
            { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF },
            { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A },
            { 'G', 'I', 'F', '8' },
            { 'B', 'M' },
    };

    private boolean esWebp(byte[] b) {
        return b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
    }

    /**
     * Pre : el contenido del archivo.
     * Post: si los primeros bytes son la firma de un JPEG, PNG, GIF o WebP.
     *       Mira el contenido real y no el Content-Type que declara el
     *       cliente, que se puede mentir.
     */
    private boolean pareceImagen(byte[] contenido) {
        if (esWebp(contenido))
            return true;

        for (byte[] firma : FIRMAS) {
            if (contenido.length < firma.length)
                continue;
            boolean coincide = true;
            for (int i = 0; i < firma.length; i++) {
                if (contenido[i] != firma[i]) {
                    coincide = false;
                    break;
                }
            }
            if (coincide)
                return true;
        }
        return false;
    }

    /**
     * Pre : el producto y los bytes de la imagen.
     * Post: el veredicto de la IA. Si el servicio no responde devuelve
     *       EN_REVISION en vez de fallar: la verificacion no puede ser un
     *       punto unico de falla para publicar.
     */
    private void verificar(Foto foto, byte[] contenido, Producto producto)
            throws FotoRechazadaException {
        VerificadorImagenService.Resultado resultado;
        try {
            resultado = verificador.verificar(contenido, producto.getCategoria());
        } catch (Exception e) {
            log.warn("No se pudo verificar la foto del producto {}: {}",
                    producto.getId(), e.getMessage());
            foto.setEstadoVerificacion(EstadoVerificacion.EN_REVISION);
            return;
        }

        double puntaje = resultado.puntaje();
        foto.setConfianzaIa(puntaje);
        foto.setQueVeIa(resultado.queVeo());

        if (puntaje < umbralRechazo)
            throw new FotoRechazadaException(resultado.mensajeAlVendedor() != null
                    ? resultado.mensajeAlVendedor()
                    : "La foto no se corresponde con la categoria del producto");

        foto.setEstadoVerificacion(puntaje > umbralAprobacion
                ? EstadoVerificacion.APROBADA
                : EstadoVerificacion.EN_REVISION);
    }

    public byte[] getContenidoById(Long idFoto) throws FotoNoEncontradaException {
        return fotoRepository.findById(idFoto)
                .orElseThrow(FotoNoEncontradaException::new)
                .getContenido();
    }

    /**
     * Pre : el id de quien pide, que tiene que ser ADMIN, y un estado
     *       opcional.
     * Post: las fotos en ese estado. Sin estado devuelve las EN_REVISION, que
     *       es la cola de trabajo.
     */
    public List<FotoResponse> getPendientesDeRevision(Long idSolicitante,
            EstadoVerificacion estado) throws UsuarioNoEncontradoException, AccesoDenegadoException {
        autorizacion.validarAdmin(idSolicitante);

        EstadoVerificacion buscado = estado == null ? EstadoVerificacion.EN_REVISION : estado;

        return fotoRepository.findByEstadoVerificacion(buscado).stream()
                .map(FotoResponse::from)
                .toList();
    }

    /**
     * Pre : el id de la foto, si se aprueba, y el id de quien pide.
     * Post: la foto resuelta a mano. Aprobar la deja visible; rechazar la
     *       elimina.
     */
    @Transactional
    public FotoResponse revisarFoto(Long idFoto, boolean aprobada, Long idSolicitante)
            throws FotoNoEncontradaException, UsuarioNoEncontradoException,
            AccesoDenegadoException {
        autorizacion.validarAdmin(idSolicitante);

        Foto foto = fotoRepository.findById(idFoto)
                .orElseThrow(FotoNoEncontradaException::new);

        if (!aprobada) {
            FotoResponse borrada = FotoResponse.from(foto);
            borrar(foto);
            return borrada;
        }

        foto.setEstadoVerificacion(EstadoVerificacion.APROBADA);
        return FotoResponse.from(fotoRepository.save(foto));
    }

    /**
     * Pre : el id de la foto y el id de quien pide.
     * Post: nada. Si era la ultima del producto, este vuelve a BORRADOR y sale
     *       del catalogo y de los carritos.
     */
    @Transactional
    public void deleteFoto(Long idFoto, Long idSolicitante)
            throws FotoNoEncontradaException, OperacionAjenaException, CuentaInactivaException, UsuarioNoEncontradoException {
        Foto foto = fotoRepository.findById(idFoto)
                .orElseThrow(FotoNoEncontradaException::new);

        autorizacion.validarDuenio(idSolicitante, foto.getProducto().getVendedor().getId());
        borrar(foto);
    }

    /**
     * Pre : la foto.
     * Post: nada. La saca primero de la coleccion del producto: con cascade y
     *       orphanRemoval, borrarla sola la revivia al hacer flush.
     */
    private void borrar(Foto foto) {
        Producto producto = foto.getProducto();
        if (producto != null && producto.getFotos() != null)
            producto.getFotos().remove(foto);

        fotoRepository.delete(foto);

        if (producto != null && fotoRepository.findByProductoId(producto.getId()).isEmpty()) {
            producto.setEstadoPublicacion(EstadoPublicacion.BORRADOR);
            productoRepository.save(producto);
            carritoService.quitarDeTodosLosCarritos(producto.getId());
        }
    }
}
