package com.uade.tpo.marketplace.controllers;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.entity.Categoria;
import com.uade.tpo.marketplace.entity.dto.CategoriaRequest;
import com.uade.tpo.marketplace.entity.dto.MensajeResponse;
import com.uade.tpo.marketplace.exceptions.CategoriaConSubcategoriasException;
import com.uade.tpo.marketplace.exceptions.CategoriaDuplicadaException;
import com.uade.tpo.marketplace.exceptions.CategoriaNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.JerarquiaInvalidaException;
import com.uade.tpo.marketplace.service.CategoriaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("categorias")
@RequiredArgsConstructor
public class CategoriasController {

    private final CategoriaService categoriaService;

    /**
     * Con ?soloRaices=true devuelve unicamente las categorias sin padre.
     */
    @GetMapping
    public ResponseEntity<List<Categoria>> getCategorias(
            @RequestParam(required = false, defaultValue = "false") boolean soloRaices) {
        return ResponseEntity.ok(soloRaices
                ? categoriaService.getCategoriasRaiz()
                : categoriaService.getCategorias());
    }

    @GetMapping("/{idCategoria}")
    public ResponseEntity<Categoria> getCategoriaById(@PathVariable Long idCategoria)
            throws CategoriaNoEncontradaException {
        return ResponseEntity.ok(categoriaService.getCategoriaById(idCategoria)
                .orElseThrow(CategoriaNoEncontradaException::new));
    }

    @GetMapping("/{idCategoria}/subcategorias")
    public ResponseEntity<List<Categoria>> getSubcategorias(@PathVariable Long idCategoria)
            throws CategoriaNoEncontradaException {
        return ResponseEntity.ok(categoriaService.getSubcategorias(idCategoria));
    }

    @PostMapping
    public ResponseEntity<Object> createCategoria(@RequestBody CategoriaRequest request)
            throws CategoriaDuplicadaException, CategoriaNoEncontradaException {
        Categoria result = categoriaService.createCategoria(request);
        return ResponseEntity.created(URI.create("/categorias/" + result.getId())).body(result);
    }

    @PutMapping("/{idCategoria}")
    public ResponseEntity<Categoria> updateCategoria(@PathVariable Long idCategoria,
            @RequestBody CategoriaRequest request)
            throws CategoriaNoEncontradaException, JerarquiaInvalidaException {
        return ResponseEntity.ok(categoriaService.updateCategoria(idCategoria, request));
    }

    @DeleteMapping("/{idCategoria}")
    public ResponseEntity<MensajeResponse> deleteCategoria(@PathVariable Long idCategoria)
            throws CategoriaNoEncontradaException, CategoriaConSubcategoriasException {
        categoriaService.deleteCategoria(idCategoria);
        return ResponseEntity.ok(new MensajeResponse("Categoria eliminada correctamente"));
    }
}
