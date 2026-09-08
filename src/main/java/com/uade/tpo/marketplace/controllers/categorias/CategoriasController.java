package com.uade.tpo.marketplace.controllers.categorias;

import com.uade.tpo.marketplace.controllers.categorias.CategoriaRequest;
import com.uade.tpo.marketplace.controllers.categorias.CategoriaResponse;
import com.uade.tpo.marketplace.controllers.common.MensajeResponse;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.uade.tpo.marketplace.entity.Usuario;

@RestController
@RequestMapping("categorias")
@RequiredArgsConstructor
public class CategoriasController {
    private final CategoriaService categoriaService;

    /**
     * Pre : nada, es publico. Con ?soloRaices=true se piden unicamente las que
     *       no tienen padre.
     * Post: la lista de categorias, cada una con su padre resuelto.
     */
    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> getCategorias(
            @RequestParam(required = false, defaultValue = "false") boolean soloRaices) {
        return ResponseEntity.ok(soloRaices
                ? categoriaService.getCategoriasRaiz()
                : categoriaService.getCategorias());
    }

    /**
     * Pre : el id en la ruta. Es publico.
     * Post: la categoria con su padre. 404 si no existe.
     */
    @GetMapping("/{idCategoria}")
    public ResponseEntity<CategoriaResponse> getCategoriaById(@PathVariable Long idCategoria)
            throws CategoriaNoEncontradaException {
        return ResponseEntity.ok(categoriaService.getCategoriaById(idCategoria));
    }

    /**
     * Pre : el id del padre en la ruta. Es publico.
     * Post: sus hijas inmediatas, sin nietas. 404 si el padre no existe.
     */
    @GetMapping("/{idCategoria}/subcategorias")
    public ResponseEntity<List<CategoriaResponse>> getSubcategorias(@PathVariable Long idCategoria)
            throws CategoriaNoEncontradaException {
        return ResponseEntity.ok(categoriaService.getSubcategorias(idCategoria));
    }

    /**
     * Pre : el body con nombre y, opcionalmente, descripcion e
     *       idCategoriaPadre. El token tiene que ser de un ADMIN.
     * Post: 201 con la categoria creada y su Location. 403 si no sos ADMIN,
     *       404 si el padre no existe, 400 si ya hay una hermana con ese mismo
     *       nombre.
     */
    @PostMapping
    public ResponseEntity<Object> createCategoria(@Valid @RequestBody CategoriaRequest request,
            @AuthenticationPrincipal Usuario usuario)
            throws CategoriaDuplicadaException, CategoriaNoEncontradaException,
            UsuarioNoEncontradoException, AccesoDenegadoException {
        CategoriaResponse result = categoriaService.createCategoria(request, usuario.getId());
        return ResponseEntity.created(URI.create("/categorias/" + result.getId())).body(result);
    }

    /**
     * Pre : el id en la ruta y el body con los datos nuevos. Mandar
     *       idCategoriaPadre la mueve. El token tiene que ser de un ADMIN.
     * Post: la categoria actualizada. 400 si el movimiento armaria un ciclo
     *       -ponerla debajo de si misma o de una de sus descendientes- o si el
     *       nombre choca con una hermana.
     */
    @PutMapping("/{idCategoria}")
    public ResponseEntity<CategoriaResponse> updateCategoria(@PathVariable Long idCategoria,
            @Valid @RequestBody CategoriaRequest request, @AuthenticationPrincipal Usuario usuario)
            throws CategoriaNoEncontradaException, JerarquiaInvalidaException,
            CategoriaDuplicadaException, UsuarioNoEncontradoException, AccesoDenegadoException {
        return ResponseEntity.ok(categoriaService.updateCategoria(idCategoria, request, usuario.getId()));
    }

    /**
     * Pre : el id en la ruta y un token de ADMIN. La categoria tiene que estar
     *       vacia: sin subcategorias y sin productos.
     * Post: un mensaje de confirmacion. 409 si tiene hijas o productos, porque
     *       borrarla dejaria registros apuntando a la nada.
     */
    @DeleteMapping("/{idCategoria}")
    public ResponseEntity<MensajeResponse> deleteCategoria(@PathVariable Long idCategoria,
            @AuthenticationPrincipal Usuario usuario)
            throws CategoriaNoEncontradaException, CategoriaConSubcategoriasException,
            CategoriaConProductosException, UsuarioNoEncontradoException,
            AccesoDenegadoException {
        categoriaService.deleteCategoria(idCategoria, usuario.getId());
        return ResponseEntity.ok(new MensajeResponse("Categoria eliminada correctamente"));
    }
}
