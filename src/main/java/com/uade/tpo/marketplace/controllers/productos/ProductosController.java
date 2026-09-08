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

/**
 * Endpoints REST del catalogo de productos.
 *
 * Recibe ProductoRequest, delega en ProductoService y devuelve
 * ProductoResponse o MensajeResponse. Los filtros de busqueda llegan como query
 * params y se pasan tal cual al service.
 */
@RestController
@RequestMapping("productos")
@RequiredArgsConstructor
public class ProductosController {

    private final ProductoService productoService;

    /**
     * El catalogo publico.
     *
     * No filtra por vendedor: para eso esta /productos/vendedor/{nombreUsuario},
     * que busca por nombre exacto. El filtro que habia aca era por coincidencia
     * parcial y mezclaba vendedores distintos.
     *
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
     * Las publicaciones propias, incluidos borradores y pausadas.
     *
     * Va antes que /{idProducto} porque Spring resuelve primero los segmentos
     * literales, pero conviene tenerlas juntas para que se vea el orden.
     *
     * El ADMIN no entra: no publica, asi que no tiene publicaciones propias.
     * Para mirar lo ajeno tiene /productos/todos.
     *
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
     * La vidriera de un vendedor, por nombre de usuario.
     *
     * El cliente no conoce ningun id, pero si el nombre de usuario: viene
     * dentro de cada producto que mira. Va antes que /{idProducto} por
     * prolijidad, aunque no chocarian: aquella es de un solo segmento.
     *
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
     * Un producto puntual.
     *
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
     * Publica un producto.
     *
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
     * Cambia los datos de un producto.
     *
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
     * Pausar o reanudar la publicacion, como en los marketplaces conocidos.
     *
     * El estado llega como enum, asi que Spring rechaza con 400 cualquier valor
     * que no exista sin que haya que validarlo a mano.
     *
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
     * Su vendedor o el ADMIN: devuelve al catalogo un producto dado de baja.
     *
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
     * Solo ADMIN: el catalogo entero, incluidos los inactivos, los borradores y
     * los pausados, que el listado publico esconde.
     *
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
     * Da de baja un producto.
     *
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
