package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.entity.dto.ProductoRequest;
import com.uade.tpo.marketplace.entity.dto.ProductoCreadoResponse;
import com.uade.tpo.marketplace.entity.dto.ProductoResponse;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.uade.tpo.marketplace.entity.Categoria;
import com.uade.tpo.marketplace.entity.EstadoPublicacion;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.CategoriaNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.OrdenamientoInvalidoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.TransicionInvalidaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.repository.CategoriaRepository;
import com.uade.tpo.marketplace.repository.ProductoRepository;
import com.uade.tpo.marketplace.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;

/**
 * Logica de productos: altas, ediciones y busquedas.
 *
 * Lo llama ProductosController y usa ProductoRepository para persistir, mas
 * CategoriaRepository y UsuarioRepository para resolver las referencias que
 * el ProductoRequest trae como ids.
 */
@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AutorizacionService autorizacion;
    private final CarritoService carritoService;

    /**
     * Cada filtro se saltea solo cuando su parametro llega en null, asi los que
     * si vienen con valor se combinan entre si.
     */
    public List<ProductoResponse> getProductos(Long idCategoria, String vendedor, String nombre,
            BigDecimal precioMin, BigDecimal precioMax, String ordenPrecio)
            throws OrdenamientoInvalidoException {

        Set<Long> ramaBuscada = idCategoria == null ? null : ramaDe(idCategoria);

        List<Producto> encontrados = productoRepository.findAll().stream()
                .filter(Producto::getActivo)
                // Un vendedor dado de baja no vende: sus publicaciones salen del
                // catalogo aunque el flag activo del producto siga en true.
                .filter(p -> p.getVendedor() != null
                        && Boolean.TRUE.equals(p.getVendedor().getActivo()))
                .filter(p -> p.getEstadoPublicacion() == EstadoPublicacion.PUBLICADO)
                // Filtrar por "Semillas" tiene que traer tambien lo que esta en
                // "Semillas > Maiz": nadie que busca en una categoria espera
                // que se le escondan los productos de sus subcategorias.
                .filter(p -> ramaBuscada == null
                        || (p.getCategoria() != null && ramaBuscada.contains(p.getCategoria().getId())))
                .filter(p -> vendedor == null
                        || (p.getVendedor() != null && p.getVendedor().getNombreUsuario()
                                .toLowerCase().contains(vendedor.toLowerCase())))
                .filter(p -> nombre == null
                        || p.getNombre().toLowerCase().contains(nombre.toLowerCase()))
                .filter(p -> precioMin == null || p.getPrecio().compareTo(precioMin) >= 0)
                .filter(p -> precioMax == null || p.getPrecio().compareTo(precioMax) <= 0)
                .toList();

        return ordenar(encontrados, ordenPrecio).stream()
                .map(ProductoResponse::from)
                .toList();
    }

    /**
     * Ordena por precio si se pidio. Sin parametro se devuelve el orden en que
     * los trajo la base, que es por id.
     */
    private List<Producto> ordenar(List<Producto> productos, String ordenPrecio)
            throws OrdenamientoInvalidoException {
        if (ordenPrecio == null)
            return productos;

        Comparator<Producto> porPrecio = Comparator.comparing(Producto::getPrecio);
        if ("desc".equalsIgnoreCase(ordenPrecio))
            porPrecio = porPrecio.reversed();
        else if (!"asc".equalsIgnoreCase(ordenPrecio))
            throw new OrdenamientoInvalidoException();

        return productos.stream().sorted(porPrecio).toList();
    }

    /**
     * La categoria pedida mas todas sus descendientes.
     *
     * Recorre el arbol hacia abajo en anchura. El arbol de categorias de un
     * marketplace tiene pocos niveles, asi que no hace falta nada mas fino.
     */
    private Set<Long> ramaDe(Long idCategoria) {
        Set<Long> rama = new LinkedHashSet<>();
        Deque<Long> pendientes = new ArrayDeque<>();
        pendientes.add(idCategoria);

        while (!pendientes.isEmpty()) {
            Long actual = pendientes.poll();
            if (!rama.add(actual))
                continue;
            for (Categoria hija : categoriaRepository.findByCategoriaPadreId(actual))
                pendientes.add(hija.getId());
        }
        return rama;
    }

    public List<ProductoResponse> getMisPublicaciones(Long idSolicitante,
            EstadoPublicacion estado) throws UsuarioNoEncontradoException {
        // Sin esto un id inexistente devolvia 200 con lista vacia, igual que un
        // vendedor real sin publicaciones. Los otros listados ya tiraban 404.
        if (!usuarioRepository.existsById(idSolicitante))
            throw new UsuarioNoEncontradoException();

        return productoRepository.findAll().stream()
                .filter(Producto::getActivo)
                .filter(p -> p.getVendedor() != null
                        && p.getVendedor().getId().equals(idSolicitante))
                .filter(p -> estado == null || p.getEstadoPublicacion() == estado)
                .map(ProductoResponse::from)
                .toList();
    }

    public ProductoResponse getProductoById(Long idProducto) throws ProductoNoEncontradoException {
        return productoRepository.findById(idProducto)
                .map(ProductoResponse::from)
                .orElseThrow(ProductoNoEncontradoException::new);
    }

    public ProductoCreadoResponse createProducto(ProductoRequest request, Long idSolicitante)
            throws CategoriaNoEncontradaException, UsuarioNoEncontradoException, CuentaInactivaException {
        // Un alta no tiene duenio previo, asi que no pasa por validarDuenio y
        // era la ultima puerta por la que una cuenta dada de baja seguia
        // publicando. El chequeo va explicito.
        autorizacion.validarActivo(idSolicitante);

        Usuario vendedor = usuarioRepository.findById(idSolicitante)
                .orElseThrow(UsuarioNoEncontradoException::new);

        Producto producto = new Producto();
        producto.setVendedor(vendedor);
        copiarDatos(producto, request);

        // Queda en BORRADOR: no se puede pedir la foto en este mismo request
        // porque la foto necesita el id que la base recien asigna ahora.
        producto = productoRepository.save(producto);
        return new ProductoCreadoResponse(ProductoResponse.from(producto),
                "El producto se guardo como borrador. Subile al menos una foto a "
                        + "POST /fotos?idProducto=" + producto.getId()
                        + " para que se publique en el catalogo.");
    }

    public ProductoResponse updateProducto(Long idProducto, ProductoRequest request,
            Long idSolicitante)
            throws ProductoNoEncontradoException, CategoriaNoEncontradaException,
            UsuarioNoEncontradoException, OperacionAjenaException, CuentaInactivaException {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(ProductoNoEncontradoException::new);

        autorizacion.validarDuenio(idSolicitante, producto.getVendedor().getId());

        copiarDatos(producto, request);
        return ProductoResponse.from(productoRepository.save(producto));
    }

    /**
     * Baja logica: el producto se marca inactivo en vez de borrarse.
     *
     * Los renglones de las ordenes ya cerradas lo referencian para trazabilidad,
     * asi que un DELETE real romperia el historial de ventas.
     */
    public ProductoResponse cambiarEstadoPublicacion(Long idProducto, EstadoPublicacion estado,
            Long idSolicitante)
            throws ProductoNoEncontradoException, OperacionAjenaException,
            TransicionInvalidaException, CuentaInactivaException, UsuarioNoEncontradoException {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(ProductoNoEncontradoException::new);

        autorizacion.validarDuenio(idSolicitante, producto.getVendedor().getId());
        validarCambioDePublicacion(producto.getEstadoPublicacion(), estado);

        producto.setEstadoPublicacion(estado);

        // Al pausar deja de estar disponible: se saca de los carritos ajenos
        // para que nadie descubra al pagar que ya no se puede comprar.
        if (estado == EstadoPublicacion.PAUSADO)
            carritoService.quitarDeTodosLosCarritos(idProducto);

        return ProductoResponse.from(productoRepository.save(producto));
    }

    /**
     * Solo se puede ir y volver entre PUBLICADO y PAUSADO.
     *
     * BORRADOR no es un estado que el vendedor elija: se entra al crear el
     * producto y se sale al subirle la primera foto, asi que ni se pausa un
     * borrador ni se vuelve a borrador a mano.
     */
    private void validarCambioDePublicacion(EstadoPublicacion actual, EstadoPublicacion nuevo)
            throws TransicionInvalidaException {
        boolean permitida = switch (actual) {
            case PUBLICADO -> nuevo == EstadoPublicacion.PAUSADO;
            case PAUSADO -> nuevo == EstadoPublicacion.PUBLICADO;
            case BORRADOR -> false;
        };

        if (!permitida)
            throw new TransicionInvalidaException();
    }

    public void deleteProducto(Long idProducto, Long idSolicitante)
            throws ProductoNoEncontradoException, OperacionAjenaException, CuentaInactivaException, UsuarioNoEncontradoException {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(ProductoNoEncontradoException::new);

        autorizacion.validarDuenio(idSolicitante, producto.getVendedor().getId());

        producto.setActivo(false);
        productoRepository.save(producto);
        carritoService.quitarDeTodosLosCarritos(idProducto);
    }

    /** El vendedor no se toca aca: lo fija el alta y despues no cambia. */
    private void copiarDatos(Producto producto, ProductoRequest request)
            throws CategoriaNoEncontradaException {
        Categoria categoria = categoriaRepository.findById(request.getIdCategoria())
                .orElseThrow(CategoriaNoEncontradaException::new);

        producto.setNombre(request.getNombre());
        producto.setPrecio(request.getPrecio());
        producto.setStock(request.getStock());
        producto.setDescripcion(request.getDescripcion());
        producto.setUbicacion(request.getUbicacion());
        producto.setDescuento(request.getDescuento() == null ? 0 : request.getDescuento());
        producto.setCategoria(categoria);

        // Las fotos no se cargan aca: se suben como archivo a POST /fotos.
    }
}
