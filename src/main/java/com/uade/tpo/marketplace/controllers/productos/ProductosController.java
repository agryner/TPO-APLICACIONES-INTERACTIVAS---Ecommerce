package com.uade.tpo.marketplace.controllers.productos;

import com.uade.tpo.marketplace.controllers.common.MensajeResponse;
import com.uade.tpo.marketplace.controllers.productos.ProductoRequest;
import com.uade.tpo.marketplace.entity.EstadoPublicacion;
import com.uade.tpo.marketplace.controllers.productos.ProductoCreadoResponse;
import com.uade.tpo.marketplace.controllers.productos.ProductoResponse;
import java.math.BigDecimal;
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

import com.uade.tpo.marketplace.exceptions.CategoriaNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.OrdenamientoInvalidoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.TransicionInvalidaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.service.ProductoService;

import lombok.RequiredArgsConstructor;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.AdminNoComerciaException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.uade.tpo.marketplace.entity.Usuario;

@RestController
@RequestMapping("productos")
@RequiredArgsConstructor
public class ProductosController {
    private final ProductoService productoService;

    /**
     * Pre : todos los filtros son opcionales y se combinan: idCategoria,
     *       nombre, precioMin, precioMax y ordenPrecio.
     * Post: los productos activos, PUBLICADOS y de vendedores vigentes.
     *       Filtrar por una categoria trae tambien los de sus descendientes.
     *       400 si ordenPrecio no es asc ni desc.
     */
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> getProductos(
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(required = false) String ordenPrecio)
            throws OrdenamientoInvalidoException {
        return ResponseEntity.ok(productoService.getProductos(
                idCategoria, nombre, precioMin, precioMax, ordenPrecio));
    }

    /**
     * Pre : el token y, opcionalmente, el estado a filtrar.
     * Post: las publicaciones propias, incluidos borradores y pausados, que el
     *       catalogo esconde. 403 si quien pide es ADMIN.
     */
    @GetMapping("/mis-publicaciones")
    public ResponseEntity<List<ProductoResponse>> getMisPublicaciones(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam(required = false) EstadoPublicacion estado)
            throws UsuarioNoEncontradoException, AdminNoComerciaException {
        return ResponseEntity.ok(productoService.getMisPublicaciones(usuario.getId(), estado));
    }

    /**
     * Pre : el nombre de usuario en la ruta. Es publico.
     * Post: las publicaciones visibles de ese vendedor, con las mismas reglas
     *       que el catalogo. 404 si no existe o esta dado de baja.
     */
    @GetMapping("/vendedor/{nombreUsuario}")
    public ResponseEntity<List<ProductoResponse>> getPorVendedor(
            @PathVariable String nombreUsuario) throws UsuarioNoEncontradoException {
        return ResponseEntity.ok(productoService.getPublicacionesDeVendedor(nombreUsuario));
    }

    /**
     * Pre : el id en la ruta. Es publico.
     * Post: el producto con su categoria, su vendedor y sus fotos. 404 si no
     *       existe.
     */
    @GetMapping("/{idProducto}")
    public ResponseEntity<ProductoResponse> getProductoById(@PathVariable Long idProducto)
            throws ProductoNoEncontradoException {
        return ResponseEntity.ok(productoService.getProductoById(idProducto));
    }

    /**
     * Pre : el body con nombre, precio, stock, descripcion, ubicacion,
     *       descuento e idCategoria, mas el token. El vendedor sale del token
     *       y no del body.
     * Post: 201 con el producto en BORRADOR y el aviso de que falta subir la
     *       foto: todavia no aparece en el catalogo. 400 si el precio no es
     *       positivo, el stock es negativo o el descuento se va de 0 a 100.
     *       403 si quien publica es ADMIN.
     */
    @PostMapping
    public ResponseEntity<Object> createProducto(@Valid @RequestBody ProductoRequest request,
            @AuthenticationPrincipal Usuario usuario)
            throws CategoriaNoEncontradaException, UsuarioNoEncontradoException, CuentaInactivaException, AdminNoComerciaException {
        ProductoCreadoResponse result = productoService.createProducto(request, usuario.getId());
        return ResponseEntity.created(URI.create("/productos/" + result.getProducto().getId()))
                .body(result);
    }

    /**
     * Pre : el id en la ruta, el body completo, y el token de su vendedor o de
     *       un ADMIN.
     * Post: el producto actualizado. No toca el estado de publicacion ni las
     *       fotos. Las ordenes ya cerradas conservan el nombre y el precio
     *       viejos.
     */
    @PutMapping("/{idProducto}")
    public ResponseEntity<ProductoResponse> updateProducto(@PathVariable Long idProducto,
            @Valid @RequestBody ProductoRequest request, @AuthenticationPrincipal Usuario usuario)
            throws ProductoNoEncontradoException, CategoriaNoEncontradaException,
            UsuarioNoEncontradoException, OperacionAjenaException, CuentaInactivaException {
        return ResponseEntity.ok(productoService.updateProducto(idProducto, request, usuario.getId()));
    }

    /**
     * Pre : el id en la ruta, el estado destino, y el token de su vendedor o
     *       de un ADMIN.
     * Post: el producto en el estado nuevo. Pausar lo saca del catalogo y de
     *       todos los carritos. 409 si sale de BORRADOR: de ahi solo se sale
     *       subiendo una foto.
     */
    @PutMapping("/{idProducto}/estado")
    public ResponseEntity<ProductoResponse> cambiarEstadoPublicacion(
            @PathVariable Long idProducto, @RequestParam EstadoPublicacion estado,
            @AuthenticationPrincipal Usuario usuario)
            throws ProductoNoEncontradoException, OperacionAjenaException,
            TransicionInvalidaException, CuentaInactivaException, UsuarioNoEncontradoException {
        return ResponseEntity.ok(
                productoService.cambiarEstadoPublicacion(idProducto, estado, usuario.getId()));
    }

    /**
     * Pre : el id en la ruta y el token de su vendedor o de un ADMIN.
     * Post: el producto activo otra vez, en el estado de publicacion que tenia
     *       antes de la baja.
     */
    @PutMapping("/{idProducto}/reactivar")
    public ResponseEntity<ProductoResponse> reactivar(@PathVariable Long idProducto,
            @AuthenticationPrincipal Usuario usuario)
            throws ProductoNoEncontradoException, OperacionAjenaException, CuentaInactivaException,
            UsuarioNoEncontradoException {
        return ResponseEntity.ok(productoService.reactivarProducto(idProducto, usuario.getId()));
    }

    /**
     * Pre : un token de ADMIN y, opcionalmente, el estado a filtrar.
     * Post: todo el catalogo sin los filtros del comprador: incluye inactivos,
     *       borradores y pausados. 403 si no sos ADMIN.
     */
    @GetMapping("/todos")
    public ResponseEntity<List<ProductoResponse>> getTodos(@AuthenticationPrincipal Usuario usuario,
            @RequestParam(required = false) EstadoPublicacion estado)
            throws UsuarioNoEncontradoException, AccesoDenegadoException {
        return ResponseEntity.ok(productoService.getTodosLosProductos(usuario.getId(), estado));
    }

    /**
     * Pre : el id en la ruta y el token de su vendedor o de un ADMIN.
     * Post: un mensaje de confirmacion. Es baja logica: sale del catalogo y de
     *       los carritos, pero las ordenes que lo referencian lo siguen
     *       mostrando.
     */
    @DeleteMapping("/{idProducto}")
    public ResponseEntity<MensajeResponse> deleteProducto(@PathVariable Long idProducto,
            @AuthenticationPrincipal Usuario usuario)
            throws ProductoNoEncontradoException, OperacionAjenaException, CuentaInactivaException, UsuarioNoEncontradoException {
        productoService.deleteProducto(idProducto, usuario.getId());
        return ResponseEntity.ok(new MensajeResponse("Producto dado de baja correctamente"));
    }
}
