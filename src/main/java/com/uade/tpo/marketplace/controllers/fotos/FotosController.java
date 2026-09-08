package com.uade.tpo.marketplace.controllers.fotos;

import com.uade.tpo.marketplace.controllers.fotos.FotoContenidoResponse;
import com.uade.tpo.marketplace.controllers.fotos.FotoResponse;
import com.uade.tpo.marketplace.entity.EstadoVerificacion;
import com.uade.tpo.marketplace.controllers.fotos.FotoUploadRequest;
import com.uade.tpo.marketplace.controllers.common.MensajeResponse;
import java.net.URI;
import java.util.Base64;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.ArchivoInvalidoException;
import com.uade.tpo.marketplace.exceptions.FotoNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.FotoRechazadaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.service.FotoService;

import lombok.RequiredArgsConstructor;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.AdminNoComerciaException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.uade.tpo.marketplace.entity.Usuario;

@RestController
@RequestMapping("fotos")
@RequiredArgsConstructor
public class FotosController {
    private final FotoService fotoService;

    /**
     * Pre : el idProducto como query param. Es publico.
     * Post: la lista de metadatos, sin los bytes: cada una trae la URL para
     *       pedir la imagen. 404 si el producto no existe, lista vacia si
     *       existe pero esta en BORRADOR.
     */
    @GetMapping
    public ResponseEntity<List<FotoResponse>> getFotos(@RequestParam Long idProducto)
            throws ProductoNoEncontradoException {
        return ResponseEntity.ok(fotoService.getFotosByProducto(idProducto));
    }

    /**
     * Pre : el id en la ruta. Es publico.
     * Post: nombre de archivo, tipo, tamanio, estado de verificacion y la URL
     *       del contenido. 404 si no existe.
     */
    @GetMapping("/{idFoto}")
    public ResponseEntity<FotoResponse> getFotoById(@PathVariable Long idFoto)
            throws FotoNoEncontradaException {
        return ResponseEntity.ok(fotoService.getFotoById(idFoto));
    }

    /**
     * Pre : un multipart con el archivo en el campo file y el idProducto, mas
     *       el token del vendedor de ese producto.
     * Post: 201 con los metadatos de la foto. Si el producto estaba en
     *       BORRADOR queda PUBLICADO. 400 si el archivo no es una imagen de
     *       verdad, 403 si el producto es de otro o si quien sube es ADMIN,
     *       409 si la IA la rechaza.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FotoResponse> subirFoto(@ModelAttribute FotoUploadRequest request,
            @AuthenticationPrincipal Usuario usuario)
            throws ProductoNoEncontradoException, ArchivoInvalidoException,
            FotoRechazadaException, OperacionAjenaException, CuentaInactivaException, UsuarioNoEncontradoException, AdminNoComerciaException {
        FotoResponse result = fotoService.subirFoto(request, usuario.getId());
        return ResponseEntity.created(URI.create("/fotos/" + result.getId())).body(result);
    }

    /**
     * Pre : el id de la foto en la ruta. Es publico.
     * Post: los bytes con su Content-Type, listos para el src de un img. 404
     *       si no existe.
     */
    @GetMapping("/{idFoto}/contenido")
    public ResponseEntity<byte[]> getContenido(@PathVariable Long idFoto)
            throws FotoNoEncontradaException {
        FotoResponse foto = fotoService.getFotoById(idFoto);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.getTipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + foto.getNombreArchivo() + "\"")
                .body(fotoService.getContenidoById(idFoto));
    }

    /**
     * Pre : el id de la foto en la ruta. Es publico.
     * Post: la misma imagen dentro de un JSON, para clientes que no pueden
     *       pedir binario.
     */
    @GetMapping("/{idFoto}/base64")
    public ResponseEntity<FotoContenidoResponse> getContenidoBase64(@PathVariable Long idFoto)
            throws FotoNoEncontradaException {
        byte[] contenido = fotoService.getContenidoById(idFoto);
        return ResponseEntity.ok(new FotoContenidoResponse(idFoto,
                Base64.getEncoder().encodeToString(contenido)));
    }

    /**
     * Pre : un token de ADMIN y, opcionalmente, el estado a filtrar.
     * Post: las fotos en ese estado. Sin estado devuelve las EN_REVISION, que
     *       es la cola de trabajo. 403 si no sos ADMIN.
     */
    @GetMapping("/pendientes")
    public ResponseEntity<List<FotoResponse>> getPendientes(@AuthenticationPrincipal Usuario usuario,
            @RequestParam(required = false) EstadoVerificacion estado)
            throws UsuarioNoEncontradoException, AccesoDenegadoException {
        return ResponseEntity.ok(fotoService.getPendientesDeRevision(usuario.getId(), estado));
    }

    /**
     * Pre : el id de la foto, el flag aprobada y un token de ADMIN.
     * Post: la foto con su estado resuelto. Aprobar la deja visible; rechazar
     *       la elimina.
     */
    @PutMapping("/{idFoto}/revision")
    public ResponseEntity<FotoResponse> revisarFoto(@PathVariable Long idFoto,
            @RequestParam boolean aprobada, @AuthenticationPrincipal Usuario usuario)
            throws FotoNoEncontradaException, UsuarioNoEncontradoException,
            AccesoDenegadoException {
        return ResponseEntity.ok(fotoService.revisarFoto(idFoto, aprobada, usuario.getId()));
    }

    /**
     * Pre : el id en la ruta y el token del vendedor del producto.
     * Post: un mensaje de confirmacion. Si era la ultima foto, el producto
     *       vuelve a BORRADOR y sale del catalogo y de los carritos.
     */
    @DeleteMapping("/{idFoto}")
    public ResponseEntity<MensajeResponse> deleteFoto(@PathVariable Long idFoto,
            @AuthenticationPrincipal Usuario usuario)
            throws FotoNoEncontradaException, OperacionAjenaException, CuentaInactivaException, UsuarioNoEncontradoException {
        fotoService.deleteFoto(idFoto, usuario.getId());
        return ResponseEntity.ok(new MensajeResponse("Foto eliminada correctamente"));
    }
}
