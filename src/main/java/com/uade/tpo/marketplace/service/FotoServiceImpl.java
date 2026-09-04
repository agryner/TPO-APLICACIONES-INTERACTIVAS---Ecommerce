package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.entity.dto.FotoResponse;
import com.uade.tpo.marketplace.entity.dto.FotoUploadRequest;
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

    private static final Logger log = LoggerFactory.getLogger(FotoServiceImpl.class);

    /** Arriba de este puntaje la foto entra directo. */
    @Value("${marketplace.ia.umbral-aprobacion:0.7}")
    private double umbralAprobacion;

    /** Debajo de este puntaje se rechaza la subida. */
    @Value("${marketplace.ia.umbral-rechazo:0.4}")
    private double umbralRechazo;

    private final FotoRepository fotoRepository;
    private final ProductoRepository productoRepository;
    private final VerificadorImagenService verificador;
    private final AutorizacionService autorizacion;
    private final CarritoService carritoService;

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

        // validarDuenio deja pasar al ADMIN, y para subir una foto no
        // corresponde: subirla es parte de publicar, y el admin no publica.
        // Modera lo que otros suben, con la cola de revision y el borrado.
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
        // El Content-Type lo elige el cliente y no prueba nada: un .txt
        // mandado como "image/jpeg" pasaba el filtro de arriba, quedaba
        // guardado como foto y encima publicaba el producto. Los primeros
        // bytes los escribe el programa que genero el archivo.
        if (!pareceImagen(contenido))
            throw new ArchivoInvalidoException(
                    "El archivo no es una imagen: su contenido no corresponde a ningun formato conocido");

        foto.setContenido(contenido);

        verificar(foto, contenido, producto);
        foto = fotoRepository.save(foto);

        // Primera foto: el producto deja de ser borrador y entra al catalogo.
        producto.setEstadoPublicacion(EstadoPublicacion.PUBLICADO);
        productoRepository.save(producto);

        return FotoResponse.from(foto);
    }

    /**
     * Pasa la foto por la verificacion automatica y decide que hacer con ella.
     *
     * Tres bandas: arriba del umbral de aprobacion entra directo, abajo del de
     * rechazo se corta la subida, y en el medio se guarda pero marcada para que
     * la revise un admin. Si la IA falla tambien va a revision: que se caiga un
     * servicio externo no puede dejar al vendedor sin poder publicar.
     */
    /**
     * Firmas de los formatos aceptados, en bytes.
     *
     * Son los primeros bytes que escribe quien genera el archivo, asi que
     * describen lo que el archivo es de verdad y no lo que el cliente dice
     * que es en la cabecera.
     */
    private static final byte[][] FIRMAS = {
            { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF },                  // JPEG
            { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A },     // PNG
            { 'G', 'I', 'F', '8' },                                     // GIF
            { 'B', 'M' },                                               // BMP
    };

    /** WEBP es un contenedor RIFF: "RIFF" + 4 bytes de tamanio + "WEBP". */
    private boolean esWebp(byte[] b) {
        return b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
    }

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

    public List<FotoResponse> getPendientesDeRevision(Long idSolicitante,
            EstadoVerificacion estado) throws UsuarioNoEncontradoException, AccesoDenegadoException {
        autorizacion.validarAdmin(idSolicitante);

        // Sin estado devuelve la cola: lo que espera decision. Con estado se
        // puede mirar lo ya resuelto, que es como el admin revisa si se
        // equivoco al aprobar o rechazar algo.
        EstadoVerificacion buscado = estado == null ? EstadoVerificacion.EN_REVISION : estado;

        return fotoRepository.findByEstadoVerificacion(buscado).stream()
                .map(FotoResponse::from)
                .toList();
    }

    @Transactional
    public FotoResponse revisarFoto(Long idFoto, boolean aprobada, Long idSolicitante)
            throws FotoNoEncontradaException, UsuarioNoEncontradoException,
            AccesoDenegadoException {
        autorizacion.validarAdmin(idSolicitante);

        Foto foto = fotoRepository.findById(idFoto)
                .orElseThrow(FotoNoEncontradaException::new);

        if (!aprobada) {
            // El admin no es el vendedor, asi que borra sin pasar por el
            // chequeo de pertenencia: moderar es justamente su atribucion.
            FotoResponse borrada = FotoResponse.from(foto);
            borrar(foto);
            return borrada;
        }

        foto.setEstadoVerificacion(EstadoVerificacion.APROBADA);
        return FotoResponse.from(fotoRepository.save(foto));
    }

    @Transactional
    public void deleteFoto(Long idFoto, Long idSolicitante)
            throws FotoNoEncontradaException, OperacionAjenaException, CuentaInactivaException, UsuarioNoEncontradoException {
        Foto foto = fotoRepository.findById(idFoto)
                .orElseThrow(FotoNoEncontradaException::new);

        autorizacion.validarDuenio(idSolicitante, foto.getProducto().getVendedor().getId());
        borrar(foto);
    }

    /**
     * Producto mapea sus fotos con cascade ALL y las carga en EAGER, asi que al
     * leer la foto tambien queda gestionada la coleccion que la contiene. Si no
     * la sacamos de ahi, el cascade PERSIST la revive en el flush y el borrado
     * se pierde sin error. Sacandola, orphanRemoval dispara el delete.
     */
    private void borrar(Foto foto) {
        Producto producto = foto.getProducto();
        if (producto != null && producto.getFotos() != null)
            producto.getFotos().remove(foto);

        fotoRepository.delete(foto);

        // Sin fotos vuelve a borrador: el catalogo nunca muestra un producto
        // sin imagen. La consulta corre despues del delete, asi que ya refleja
        // el borrado: si queda alguna, la publicacion sigue en pie.
        if (producto != null && fotoRepository.findByProductoId(producto.getId()).isEmpty()) {
            producto.setEstadoPublicacion(EstadoPublicacion.BORRADOR);
            productoRepository.save(producto);
            carritoService.quitarDeTodosLosCarritos(producto.getId());
        }
    }
}
