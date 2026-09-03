package com.uade.tpo.marketplace.controllers;

import com.uade.tpo.marketplace.entity.dto.CategoriaRequest;
import com.uade.tpo.marketplace.entity.dto.CategoriaResponse;
import com.uade.tpo.marketplace.entity.dto.MensajeResponse;
import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.exceptions.CategoriaConProductosException;
import com.uade.tpo.marketplace.exceptions.CategoriaConSubcategoriasException;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.CategoriaDuplicadaException;
import com.uade.tpo.marketplace.exceptions.CategoriaNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.JerarquiaInvalidaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.service.CategoriaService;

import lombok.RequiredArgsConstructor;

/**
 * Endpoints REST del arbol de categorias.
 *
 * Recibe CategoriaRequest desde el body, delega todo en CategoriaService y
 * devuelve CategoriaResponse o MensajeResponse. No tiene logica propia:
 * solo traduce HTTP a llamadas al service.
 *
 * El alta, la edicion y la baja estan restringidas a administradores: el id
 * de quien las pide viaja como query param idSolicitante y lo valida el service.
 */
@RestController
@RequestMapping("categorias")
@RequiredArgsConstructor
public class CategoriasController {

    private final CategoriaService categoriaService;

    /**
     * Con ?soloRaices=true devuelve unicamente las categorias sin padre.
     */
    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> getCategorias(
            @RequestParam(required = false, defaultValue = "false") boolean soloRaices) {
        return ResponseEntity.ok(soloRaices
                ? categoriaService.getCategoriasRaiz()
                : categoriaService.getCategorias());
    }

    @GetMapping("/{idCategoria}")
    public ResponseEntity<CategoriaResponse> getCategoriaById(@PathVariable Long idCategoria)
            throws CategoriaNoEncontradaException {
        return ResponseEntity.ok(categoriaService.getCategoriaById(idCategoria));
    }

    @GetMapping("/{idCategoria}/subcategorias")
    public ResponseEntity<List<CategoriaResponse>> getSubcategorias(@PathVariable Long idCategoria)
            throws CategoriaNoEncontradaException {
        return ResponseEntity.ok(categoriaService.getSubcategorias(idCategoria));
    }

    @PostMapping
    public ResponseEntity<Object> createCategoria(@Valid @RequestBody CategoriaRequest request,
            @RequestParam Long idSolicitante)
            throws CategoriaDuplicadaException, CategoriaNoEncontradaException,
            UsuarioNoEncontradoException, AccesoDenegadoException {
        CategoriaResponse result = categoriaService.createCategoria(request, idSolicitante);
        return ResponseEntity.created(URI.create("/categorias/" + result.getId())).body(result);
    }

    @PutMapping("/{idCategoria}")
    public ResponseEntity<CategoriaResponse> updateCategoria(@PathVariable Long idCategoria,
            @Valid @RequestBody CategoriaRequest request, @RequestParam Long idSolicitante)
            throws CategoriaNoEncontradaException, JerarquiaInvalidaException,
            CategoriaDuplicadaException, UsuarioNoEncontradoException, AccesoDenegadoException {
        return ResponseEntity.ok(categoriaService.updateCategoria(idCategoria, request, idSolicitante));
    }

    @DeleteMapping("/{idCategoria}")
    public ResponseEntity<MensajeResponse> deleteCategoria(@PathVariable Long idCategoria,
            @RequestParam Long idSolicitante)
            throws CategoriaNoEncontradaException, CategoriaConSubcategoriasException,
            CategoriaConProductosException, UsuarioNoEncontradoException,
            AccesoDenegadoException {
        categoriaService.deleteCategoria(idCategoria, idSolicitante);
        return ResponseEntity.ok(new MensajeResponse("Categoria eliminada correctamente"));
    }
}
