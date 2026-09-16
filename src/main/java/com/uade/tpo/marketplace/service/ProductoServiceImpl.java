package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.productos.FiltroProductos;
import com.uade.tpo.marketplace.controllers.productos.ProductoRequest;
import com.uade.tpo.marketplace.controllers.productos.ProductoCreadoResponse;
import com.uade.tpo.marketplace.controllers.productos.ProductoResponse;
import com.uade.tpo.marketplace.controllers.productos.ProductoResumenResponse;
import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.marketplace.entity.Categoria;
import com.uade.tpo.marketplace.entity.TipoNotificacion;
import com.uade.tpo.marketplace.entity.EstadoPublicacion;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.AnioInvalidoException;
import com.uade.tpo.marketplace.exceptions.CategoriaNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.OrdenamientoInvalidoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.RolNoComerciaException;
import com.uade.tpo.marketplace.exceptions.TransicionInvalidaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.repository.CategoriaRepository;
import com.uade.tpo.marketplace.repository.ProductoRepository;
import com.uade.tpo.marketplace.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final NotificacionService notificacionService;
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
    public List<ProductoResumenResponse> getProductos(FiltroProductos filtro)
            throws OrdenamientoInvalidoException, SinResultadosException {
        Set<Long> ramaBuscada = filtro.getIdCategoria() == null
                ? null
                : ramaDe(filtro.getIdCategoria());
        String nombre = filtro.getNombre();

        List<Producto> encontrados = productoRepository.findAll().stream()
                .filter(Producto::getActivo)
                .filter(p -> p.getVendedor() != null
                        && Boolean.TRUE.equals(p.getVendedor().getActivo()))
                .filter(p -> p.getEstadoPublicacion() == EstadoPublicacion.PUBLICADO)
                .filter(p -> ramaBuscada == null
                        || (p.getCategoria() != null && ramaBuscada.contains(p.getCategoria().getId())))
                .filter(p -> nombre == null
                        || p.getNombre().toLowerCase().contains(nombre.toLowerCase()))
                .filter(p -> filtro.getPrecioMin() == null
                        || p.getPrecio().compareTo(filtro.getPrecioMin()) >= 0)
                .filter(p -> filtro.getPrecioMax() == null
                        || p.getPrecio().compareTo(filtro.getPrecioMax()) <= 0)
                .filter(p -> !Boolean.TRUE.equals(filtro.getEnOferta())
                        || (p.getDescuento() != null && p.getDescuento() > 0))
                .filter(p -> filtro.getProvincia() == null
                        || p.getProvincia() == filtro.getProvincia())
                .filter(p -> filtro.getCondicion() == null
                        || p.getCondicion() == filtro.getCondicion())
                .filter(p -> filtro.getAnioDesde() == null
                        || (p.getAnio() != null && p.getAnio() >= filtro.getAnioDesde()))
                .filter(p -> filtro.getAnioHasta() == null
                        || (p.getAnio() != null && p.getAnio() <= filtro.getAnioHasta()))
                // Sin envio es un filtro tan valido como con envio: el que
                // compra un tractor busca justo los que se retiran.
                .filter(p -> filtro.getAdmiteEnvio() == null
                        || Boolean.TRUE.equals(p.getAdmiteEnvio()) == filtro.getAdmiteEnvio())
                .toList();

        if (encontrados.isEmpty())
            throw new SinResultadosException("No hay productos que coincidan con la busqueda");

        return ordenar(encontrados, filtro.getOrden()).stream()
                .map(ProductoResumenResponse::from)
                .toList();
    }

    /**
     * Pre : la lista y el criterio, asc o desc.
     * Post: la lista ordenada por precio, o tal cual si no se pidio orden.
     *       Tira OrdenamientoInvalidoException con cualquier otro valor.
     */
    /**
     * Pre : los productos y el criterio, que puede venir en null.
     * Post: la lista ordenada. Un solo parametro para todos los criterios: con
     *       uno por criterio habria que inventar una regla de prioridad para
     *       cuando llegan dos, y esa regla hay que explicarla. Tira
     *       OrdenamientoInvalidoException si el criterio no existe.
     */
    private List<Producto> ordenar(List<Producto> productos, String orden)
            throws OrdenamientoInvalidoException {
        // Sin criterio explicito manda la visibilidad pagada. Con criterio, no
        // interviene: si alguien pidio precio_asc y le pusieramos los PREMIUM
        // arriba, la lista no estaria ordenada por precio aunque lo haya
        // pedido.
        if (orden == null)
            return productos.stream()
                    .sorted(Comparator.comparingInt(
                            (Producto p) -> p.nivelVigente().getPeso()).reversed())
                    .toList();

        Comparator<Producto> criterio = switch (orden.toLowerCase()) {
            case "precio_asc" -> Comparator.comparing(Producto::getPrecio);
            case "precio_desc" -> Comparator.comparing(Producto::getPrecio).reversed();
            case "vistos" -> Comparator.comparing(
                    (Producto p) -> p.getVistos() == null ? 0 : p.getVistos()).reversed();
            case "vendidos" -> Comparator.comparing(
                    (Producto p) -> p.getVendidos() == null ? 0 : p.getVendidos()).reversed();
            default -> throw new OrdenamientoInvalidoException();
        };

        return productos.stream().sorted(criterio).toList();
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
            for (Categoria hija : categoriaRepository.findByCategoriaPadreIdAndActivoTrue(actual))
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
            throws UsuarioNoEncontradoException, RolNoComerciaException, SinResultadosException {
        if (!usuarioRepository.existsById(idSolicitante))
            throw new UsuarioNoEncontradoException();

        autorizacion.validarQuePuedaComerciar(idSolicitante);

        List<ProductoResponse> propias = productoRepository.findAll().stream()
                .filter(Producto::getActivo)
                .filter(p -> p.getVendedor() != null
                        && p.getVendedor().getId().equals(idSolicitante))
                .filter(p -> estado == null || p.getEstadoPublicacion() == estado)
                .map(ProductoResponse::from)
                .toList();

        if (propias.isEmpty())
            throw new SinResultadosException("Todavia no tenes publicaciones");

        return propias;
    }

    /**
     * Pre : el id del producto y el de quien lo abre, que viene en null si no
     *       hay token porque el detalle es publico.
     * Post: el producto, con una visita mas. No se cuenta la del propio
     *       vendedor: si no, refrescar su publicacion le infla el numero por
     *       el que despues se ordena el catalogo.
     */
    @Transactional
    public ProductoResponse getProductoById(Long idProducto, Long idSolicitante)
            throws ProductoNoEncontradoException {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(ProductoNoEncontradoException::new);

        boolean esElVendedor = idSolicitante != null
                && producto.getVendedor() != null
                && producto.getVendedor().getId().equals(idSolicitante);

        if (!esElVendedor) {
            producto.setVistos((producto.getVistos() == null ? 0 : producto.getVistos()) + 1);
            producto = productoRepository.save(producto);
        }

        return ProductoResponse.from(producto);
    }

    /**
     * Pre : el id de un producto.
     * Post: hasta 8 publicaciones parecidas y disponibles, sin incluirse a si
     *       misma. Busca primero entre sus hermanas de categoria y, si no
     *       llena, sube al padre: una categoria hoja con un solo producto
     *       siempre devolveria vacio.
     */
    public List<ProductoResumenResponse> getSimilares(Long idProducto)
            throws ProductoNoEncontradoException, SinResultadosException {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(ProductoNoEncontradoException::new);

        Categoria categoria = producto.getCategoria();
        Long raiz = categoria == null ? null
                : (categoria.getCategoriaPadre() == null ? categoria.getId()
                        : categoria.getCategoriaPadre().getId());

        Set<Long> rama = raiz == null ? Set.of() : ramaDe(raiz);

        List<ProductoResumenResponse> similares = productoRepository.findAll().stream()
                .filter(p -> !p.getId().equals(idProducto))
                .filter(ProductoResumenResponse::estaDisponible)
                .filter(p -> p.getCategoria() != null && rama.contains(p.getCategoria().getId()))
                .sorted(Comparator.comparing(
                        (Producto p) -> p.getVendidos() == null ? 0 : p.getVendidos()).reversed())
                .limit(8)
                .map(ProductoResumenResponse::from)
                .toList();

        if (similares.isEmpty())
            throw new SinResultadosException("No hay productos similares a este");

        return similares;
    }

    /**
     * Pre : el request y el id del vendedor.
     * Post: el producto en BORRADOR mas el aviso de que falta la foto. Todavia
     *       no aparece en el catalogo. Tira RolNoComerciaException si quien
     *       publica es ADMIN.
     */
    public ProductoCreadoResponse createProducto(ProductoRequest request, Long idSolicitante)
            throws CategoriaNoEncontradaException, UsuarioNoEncontradoException,
            CuentaInactivaException, RolNoComerciaException, AnioInvalidoException {
        autorizacion.validarActivo(idSolicitante);

        autorizacion.validarQuePuedaComerciar(idSolicitante);

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
            UsuarioNoEncontradoException, OperacionAjenaException, CuentaInactivaException,
            AnioInvalidoException {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(ProductoNoEncontradoException::new);

        autorizacion.validarDuenio(idSolicitante, producto.getVendedor().getId());

        BigDecimal precioViejo = ProductoResumenResponse.conDescuento(producto);

        copiarDatos(producto, request);
        Producto guardado = productoRepository.save(producto);

        avisarSiBajoElPrecio(guardado, precioViejo);
        return ProductoResponse.from(guardado);
    }

    /**
     * Pre : el producto ya guardado y el precio final que tenia antes.
     * Post: nada. Si el precio final quedo mas bajo, les avisa a todos los que
     *       lo tienen en su wishlist. Las subas no se notifican: es una mala
     *       noticia que nadie pidio.
     */
    private void avisarSiBajoElPrecio(Producto producto, BigDecimal precioViejo) {
        BigDecimal precioNuevo = ProductoResumenResponse.conDescuento(producto);

        if (precioViejo == null || precioNuevo == null
                || precioNuevo.compareTo(precioViejo) >= 0)
            return;

        notificacionService.avisarAQuienesLoTienenGuardado(producto,
                TipoNotificacion.BAJA_DE_PRECIO,
                "\"%s\" bajo de $%s a $%s".formatted(producto.getNombre(),
                        precioViejo.toPlainString(), precioNuevo.toPlainString()));
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

        Producto guardado = productoRepository.save(producto);

        if (estado == EstadoPublicacion.PUBLICADO)
            avisarSiVolvio(guardado);

        return ProductoResponse.from(guardado);
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
        Producto guardado = productoRepository.save(producto);

        avisarSiVolvio(guardado);
        return ProductoResponse.from(guardado);
    }

    /**
     * Pre : el producto ya guardado.
     * Post: nada. Si quedo comprable otra vez, les avisa a los que lo tienen
     *       en su wishlist. Se chequea el estado real y no solo la accion que
     *       se pidio: despausar algo cuyo vendedor esta dado de baja no lo
     *       vuelve disponible, y avisarlo seria mentir.
     */
    private void avisarSiVolvio(Producto producto) {
        if (!ProductoResumenResponse.estaDisponible(producto))
            return;

        notificacionService.avisarAQuienesLoTienenGuardado(producto,
                TipoNotificacion.DISPONIBLE_OTRA_VEZ,
                "\"%s\" volvio a estar disponible".formatted(producto.getNombre()));
    }

    /**
     * Pre : el nombre de usuario del vendedor. Es publico.
     * Post: sus publicaciones visibles. 404 si no existe un vendedor con ese
     *       nombre, o si esta dado de baja: para quien mira el catalogo, una
     *       cuenta de baja es una cuenta que no esta.
     */
    public List<ProductoResumenResponse> getPublicacionesDeVendedor(String nombreUsuario)
            throws UsuarioNoEncontradoException, SinResultadosException {
        Usuario vendedor = usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);

        if (!Boolean.TRUE.equals(vendedor.getActivo()))
            throw new UsuarioNoEncontradoException();

        List<ProductoResumenResponse> vidriera = productoRepository.findAll().stream()
                .filter(Producto::getActivo)
                .filter(p -> p.getVendedor() != null
                        && p.getVendedor().getId().equals(vendedor.getId()))
                .filter(p -> p.getEstadoPublicacion() == EstadoPublicacion.PUBLICADO)
                .map(ProductoResumenResponse::from)
                .toList();

        if (vidriera.isEmpty())
            throw new SinResultadosException("Ese vendedor todavia no tiene publicaciones visibles");

        return vidriera;
    }

    /**
     * Pre : el id de quien pide, que tiene que ser ADMIN, y un estado
     *       opcional.
     * Post: todo el catalogo sin los filtros del comprador: inactivos,
     *       borradores y pausados incluidos.
     */
    public List<ProductoResponse> getTodosLosProductos(Long idSolicitante, EstadoPublicacion estado)
            throws UsuarioNoEncontradoException, AccesoDenegadoException, SinResultadosException {
        autorizacion.validarAdmin(idSolicitante);

        List<ProductoResponse> todos = productoRepository.findAll().stream()
                .filter(p -> estado == null || p.getEstadoPublicacion() == estado)
                .map(ProductoResponse::from)
                .toList();

        if (todos.isEmpty())
            throw new SinResultadosException("No hay productos cargados");

        return todos;
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
            throws CategoriaNoEncontradaException, AnioInvalidoException {
        // El tope no es una constante: es el anio corriente, asi que sube solo
        // cada 1 de enero.
        int maximo = Year.now().getValue();

        if (request.getAnio() > maximo)
            throw new AnioInvalidoException(maximo);

        Categoria categoria = categoriaRepository.findById(request.getIdCategoria())
                .filter(c -> Boolean.TRUE.equals(c.getActivo()))
                .orElseThrow(CategoriaNoEncontradaException::new);

        producto.setNombre(request.getNombre());
        producto.setPrecio(request.getPrecio());
        producto.setStock(request.getStock());
        producto.setAdmiteEnvio(request.getAdmiteEnvio() == null
                || Boolean.TRUE.equals(request.getAdmiteEnvio()));
        // Al reves que el envio: aceptar ofertas hay que pedirlo. Si no se
        // manda nada, el producto no se negocia.
        producto.setAceptaOfertas(Boolean.TRUE.equals(request.getAceptaOfertas()));
        producto.setDescripcion(request.getDescripcion());
        producto.setProvincia(request.getProvincia());
        producto.setUbicacion(request.getUbicacion());
        producto.setCondicion(request.getCondicion());
        producto.setAnio(request.getAnio());
        producto.setDescuento(request.getDescuento() == null ? 0 : request.getDescuento());
        producto.setCategoria(categoria);
    }
}
