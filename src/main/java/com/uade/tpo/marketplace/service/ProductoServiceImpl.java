package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.productos.ProductoRequest;
import com.uade.tpo.marketplace.controllers.productos.ProductoCreadoResponse;
import com.uade.tpo.marketplace.controllers.productos.ProductoResponse;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.marketplace.entity.Categoria;
import com.uade.tpo.marketplace.entity.EstadoPublicacion;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.CategoriaNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.OrdenamientoInvalidoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.AdminNoComerciaException;
import com.uade.tpo.marketplace.exceptions.TransicionInvalidaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.repository.CategoriaRepository;
import com.uade.tpo.marketplace.repository.ProductoRepository;
import com.uade.tpo.marketplace.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AutorizacionService autorizacion;
    private final CarritoService carritoService;

    /**
     * Pre : los filtros, todos opcionales: categoria, nombre, rango de precio
     *       y orden. nombre busca por coincidencia parcial y sin distinguir
     *       mayusculas; ordenPrecio acepta "asc" o "desc", y en null se
     *       respeta el orden de la base.
     * Post: los productos activos, PUBLICADOS y de vendedores vigentes que
     *       cumplen todos los filtros. Filtrar por una categoria incluye a sus
     *       descendientes.
     */
    public List<ProductoResponse> getProductos(Long idCategoria, String nombre,
            BigDecimal precioMin, BigDecimal precioMax, String ordenPrecio)
            throws OrdenamientoInvalidoException {
        Set<Long> ramaBuscada = idCategoria == null ? null : ramaDe(idCategoria);

        List<Producto> encontrados = productoRepository.findAll().stream()
                .filter(Producto::getActivo)
                .filter(p -> p.getVendedor() != null
                        && Boolean.TRUE.equals(p.getVendedor().getActivo()))
                .filter(p -> p.getEstadoPublicacion() == EstadoPublicacion.PUBLICADO)
                .filter(p -> ramaBuscada == null
                        || (p.getCategoria() != null && ramaBuscada.contains(p.getCategoria().getId())))
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
     * Pre : la lista y el criterio, asc o desc.
     * Post: la lista ordenada por precio, o tal cual si no se pidio orden.
     *       Tira OrdenamientoInvalidoException con cualquier otro valor.
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
     * Pre : el id de una categoria.
     * Post: ese id mas los de todas sus descendientes. Sin esto, filtrar por
     *       una categoria padre no traeria nada de sus hijas.
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

    /**
     * Pre : el id del vendedor y, opcionalmente, el estado.
     * Post: sus publicaciones, incluidos borradores y pausados. 403 si quien
     *       pide es ADMIN. Tira UsuarioNoEncontradoException si el id no
     *       existe, para no confundirlo con un vendedor sin productos.
     */
    public List<ProductoResponse> getMisPublicaciones(Long idSolicitante,
            EstadoPublicacion estado)
            throws UsuarioNoEncontradoException, AdminNoComerciaException {
        if (!usuarioRepository.existsById(idSolicitante))
            throw new UsuarioNoEncontradoException();

        autorizacion.validarQueNoSeaAdmin(idSolicitante);

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

    /**
     * Pre : el request y el id del vendedor.
     * Post: el producto en BORRADOR mas el aviso de que falta la foto. Todavia
     *       no aparece en el catalogo. Tira AdminNoComerciaException si quien
     *       publica es ADMIN.
     */
    public ProductoCreadoResponse createProducto(ProductoRequest request, Long idSolicitante)
            throws CategoriaNoEncontradaException, UsuarioNoEncontradoException, CuentaInactivaException, AdminNoComerciaException {
        autorizacion.validarActivo(idSolicitante);

        autorizacion.validarQueNoSeaAdmin(idSolicitante);

        Usuario vendedor = usuarioRepository.findById(idSolicitante)
                .orElseThrow(UsuarioNoEncontradoException::new);

        Producto producto = new Producto();
        producto.setVendedor(vendedor);
        copiarDatos(producto, request);

        producto = productoRepository.save(producto);
        return new ProductoCreadoResponse(ProductoResponse.from(producto),
                "El producto se guardo como borrador. Subile al menos una foto a "
                        + "POST /fotos?idProducto=" + producto.getId()
                        + " para que se publique en el catalogo.");
    }

    /**
     * Pre : el id, el request y el id de quien pide.
     * Post: el producto con los datos nuevos. No toca el estado de
     *       publicacion. Las ordenes ya cerradas conservan el precio viejo.
     */
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
     * Pre : el id, el estado destino y el id de quien pide.
     * Post: el producto en el estado nuevo. Pausar lo saca de todos los
     *       carritos. Tira TransicionInvalidaException si se intenta salir de
     *       BORRADOR: de ahi solo se sale subiendo una foto.
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

        if (estado == EstadoPublicacion.PAUSADO)
            carritoService.quitarDeTodosLosCarritos(idProducto);

        return ProductoResponse.from(productoRepository.save(producto));
    }

    /**
     * Pre : el estado actual y el destino.
     * Post: nada si el salto existe. Solo son validos PUBLICADO a PAUSADO y al
     *       reves.
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

    /**
     * Pre : el id y el id de quien pide.
     * Post: el producto activo otra vez, en el estado de publicacion que
     *       tenia.
     */
    @Transactional
    public ProductoResponse reactivarProducto(Long idProducto, Long idSolicitante)
            throws ProductoNoEncontradoException, OperacionAjenaException, CuentaInactivaException,
            UsuarioNoEncontradoException {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(ProductoNoEncontradoException::new);

        autorizacion.validarDuenio(idSolicitante, producto.getVendedor().getId());

        producto.setActivo(true);
        return ProductoResponse.from(productoRepository.save(producto));
    }

    /**
     * Pre : el nombre de usuario del vendedor. Es publico.
     * Post: sus publicaciones visibles. 404 si no existe un vendedor con ese
     *       nombre, o si esta dado de baja: para quien mira el catalogo, una
     *       cuenta de baja es una cuenta que no esta.
     */
    public List<ProductoResponse> getPublicacionesDeVendedor(String nombreUsuario)
            throws UsuarioNoEncontradoException {
        Usuario vendedor = usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);

        if (!Boolean.TRUE.equals(vendedor.getActivo()))
            throw new UsuarioNoEncontradoException();

        return productoRepository.findAll().stream()
                .filter(Producto::getActivo)
                .filter(p -> p.getVendedor() != null
                        && p.getVendedor().getId().equals(vendedor.getId()))
                .filter(p -> p.getEstadoPublicacion() == EstadoPublicacion.PUBLICADO)
                .map(ProductoResponse::from)
                .toList();
    }

    /**
     * Pre : el id de quien pide, que tiene que ser ADMIN, y un estado
     *       opcional.
     * Post: todo el catalogo sin los filtros del comprador: inactivos,
     *       borradores y pausados incluidos.
     */
    public List<ProductoResponse> getTodosLosProductos(Long idSolicitante, EstadoPublicacion estado)
            throws UsuarioNoEncontradoException, AccesoDenegadoException {
        autorizacion.validarAdmin(idSolicitante);

        return productoRepository.findAll().stream()
                .filter(p -> estado == null || p.getEstadoPublicacion() == estado)
                .map(ProductoResponse::from)
                .toList();
    }

    /**
     * Pre : el id y el id de quien pide.
     * Post: nada. Baja logica: el producto sale del catalogo y de los
     *       carritos, pero las ordenes lo siguen mostrando.
     */
    public void deleteProducto(Long idProducto, Long idSolicitante)
            throws ProductoNoEncontradoException, OperacionAjenaException, CuentaInactivaException, UsuarioNoEncontradoException {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(ProductoNoEncontradoException::new);

        autorizacion.validarDuenio(idSolicitante, producto.getVendedor().getId());

        producto.setActivo(false);
        productoRepository.save(producto);
        carritoService.quitarDeTodosLosCarritos(idProducto);
    }

    /**
     * Pre : el producto y el request.
     * Post: nada. Vuelca los campos del request sobre la entidad, resolviendo
     *       la categoria contra su repositorio.
     */
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
    }
}
