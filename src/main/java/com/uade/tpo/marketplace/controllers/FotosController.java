package com.uade.tpo.marketplace.controllers;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.entity.Foto;
import com.uade.tpo.marketplace.exceptions.ArchivoInvalidoException;
import com.uade.tpo.marketplace.exceptions.FotoNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.service.FotoService;

import lombok.RequiredArgsConstructor;

/**
 * Endpoints REST de las fotos de un producto.
 *
 * El alta entra como multipart/form-data en FotoUploadRequest y se delega en
 * FotoService. Para leer la imagen hay dos salidas: /contenido devuelve los
 * bytes con su Content-Type y /base64 los envuelve en un FotoResponse.
 */
@RestController
@RequestMapping("fotos")
@RequiredArgsConstructor
public class FotosController {

    private final FotoService fotoService;

    @GetMapping
    public ResponseEntity<List<Foto>> getFotos(@RequestParam Long idProducto) {
        return ResponseEntity.ok(fotoService.getFotosByProducto(idProducto));
    }

    @GetMapping("/{idFoto}")
    public ResponseEntity<Foto> getFotoById(@PathVariable Long idFoto)
            throws FotoNoEncontradaException {
        return ResponseEntity.ok(fotoService.getFotoById(idFoto)
                .orElseThrow(FotoNoEncontradaException::new));
    }

    /**
     * Unica forma de dar de alta una foto: subiendo el archivo. Se manda como
     * multipart/form-data con dos campos, 'file' con la imagen y 'idProducto'
     * con el producto al que pertenece.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Foto> subirFoto(@ModelAttribute FotoUploadRequest request)
            throws ProductoNoEncontradoException, ArchivoInvalidoException {
        Foto result = fotoService.subirFoto(request);
        return ResponseEntity.created(URI.create("/fotos/" + result.getId())).body(result);
    }

    /**
     * Devuelve la imagen tal cual, con su Content-Type original, asi que sirve
     * directo en un <img src="...">.
     */
    @GetMapping("/{idFoto}/contenido")
    public ResponseEntity<byte[]> getContenido(@PathVariable Long idFoto)
            throws FotoNoEncontradaException {
        Foto foto = fotoService.getFotoById(idFoto)
                .orElseThrow(FotoNoEncontradaException::new);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.getTipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + foto.getNombreArchivo() + "\"")
                .body(foto.getContenido());
    }

    /** La misma imagen pero en Base64 dentro de un JSON. */
    @GetMapping("/{idFoto}/base64")
    public ResponseEntity<FotoResponse> getContenidoBase64(@PathVariable Long idFoto)
            throws FotoNoEncontradaException {
        byte[] contenido = fotoService.getContenidoById(idFoto);
        return ResponseEntity.ok(new FotoResponse(idFoto,
                Base64.getEncoder().encodeToString(contenido)));
    }

    @DeleteMapping("/{idFoto}")
    public ResponseEntity<MensajeResponse> deleteFoto(@PathVariable Long idFoto)
            throws FotoNoEncontradaException {
        fotoService.deleteFoto(idFoto);
        return ResponseEntity.ok(new MensajeResponse("Foto eliminada correctamente"));
    }
}
