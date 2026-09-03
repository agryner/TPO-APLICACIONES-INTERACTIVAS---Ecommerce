package com.uade.tpo.marketplace.controllers;

import com.uade.tpo.marketplace.entity.dto.FotoContenidoResponse;
import com.uade.tpo.marketplace.entity.dto.FotoResponse;
import com.uade.tpo.marketplace.entity.EstadoVerificacion;
import com.uade.tpo.marketplace.entity.dto.FotoUploadRequest;
import com.uade.tpo.marketplace.entity.dto.MensajeResponse;
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
    public ResponseEntity<List<FotoResponse>> getFotos(@RequestParam Long idProducto)
            throws ProductoNoEncontradoException {
        return ResponseEntity.ok(fotoService.getFotosByProducto(idProducto));
    }

    @GetMapping("/{idFoto}")
    public ResponseEntity<FotoResponse> getFotoById(@PathVariable Long idFoto)
            throws FotoNoEncontradaException {
        return ResponseEntity.ok(fotoService.getFotoById(idFoto));
    }

    /**
     * Unica forma de dar de alta una foto: subiendo el archivo. Se manda como
     * multipart/form-data con dos campos, 'file' con la imagen y 'idProducto'
     * con el producto al que pertenece.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FotoResponse> subirFoto(@ModelAttribute FotoUploadRequest request,
            @RequestParam Long idSolicitante)
            throws ProductoNoEncontradoException, ArchivoInvalidoException,
            FotoRechazadaException, OperacionAjenaException, CuentaInactivaException, UsuarioNoEncontradoException {
        FotoResponse result = fotoService.subirFoto(request, idSolicitante);
        return ResponseEntity.created(URI.create("/fotos/" + result.getId())).body(result);
    }

    /**
     * Devuelve la imagen tal cual, con su Content-Type original, asi que sirve
     * directo en un <img src="...">.
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

    /** La misma imagen pero en Base64 dentro de un JSON. */
    @GetMapping("/{idFoto}/base64")
    public ResponseEntity<FotoContenidoResponse> getContenidoBase64(@PathVariable Long idFoto)
            throws FotoNoEncontradaException {
        byte[] contenido = fotoService.getContenidoById(idFoto);
        return ResponseEntity.ok(new FotoContenidoResponse(idFoto,
                Base64.getEncoder().encodeToString(contenido)));
    }

    /** Cola de revision del admin: las fotos que la IA no pudo resolver sola. */
    @GetMapping("/pendientes")
    public ResponseEntity<List<FotoResponse>> getPendientes(@RequestParam Long idSolicitante,
            @RequestParam(required = false) EstadoVerificacion estado)
            throws UsuarioNoEncontradoException, AccesoDenegadoException {
        return ResponseEntity.ok(fotoService.getPendientesDeRevision(idSolicitante, estado));
    }

    /** Aprobar deja la foto visible; rechazar la elimina. */
    @PutMapping("/{idFoto}/revision")
    public ResponseEntity<FotoResponse> revisarFoto(@PathVariable Long idFoto,
            @RequestParam boolean aprobada, @RequestParam Long idSolicitante)
            throws FotoNoEncontradaException, UsuarioNoEncontradoException,
            AccesoDenegadoException {
        return ResponseEntity.ok(fotoService.revisarFoto(idFoto, aprobada, idSolicitante));
    }

    @DeleteMapping("/{idFoto}")
    public ResponseEntity<MensajeResponse> deleteFoto(@PathVariable Long idFoto,
            @RequestParam Long idSolicitante)
            throws FotoNoEncontradaException, OperacionAjenaException, CuentaInactivaException, UsuarioNoEncontradoException {
        fotoService.deleteFoto(idFoto, idSolicitante);
        return ResponseEntity.ok(new MensajeResponse("Foto eliminada correctamente"));
    }
}
