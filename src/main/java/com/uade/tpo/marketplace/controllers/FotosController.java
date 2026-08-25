package com.uade.tpo.marketplace.controllers;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.entity.Foto;
import com.uade.tpo.marketplace.entity.dto.FotoRequest;
import com.uade.tpo.marketplace.entity.dto.MensajeResponse;
import com.uade.tpo.marketplace.exceptions.FotoNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.service.FotoService;

import lombok.RequiredArgsConstructor;

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

    @PostMapping
    public ResponseEntity<Object> createFoto(@RequestBody FotoRequest request)
            throws ProductoNoEncontradoException {
        Foto result = fotoService.createFoto(request);
        return ResponseEntity.created(URI.create("/fotos/" + result.getId())).body(result);
    }

    @DeleteMapping("/{idFoto}")
    public ResponseEntity<MensajeResponse> deleteFoto(@PathVariable Long idFoto)
            throws FotoNoEncontradaException {
        fotoService.deleteFoto(idFoto);
        return ResponseEntity.ok(new MensajeResponse("Foto eliminada correctamente"));
    }
}
