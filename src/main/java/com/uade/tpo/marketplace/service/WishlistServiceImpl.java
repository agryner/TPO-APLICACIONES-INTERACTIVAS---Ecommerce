package com.uade.tpo.marketplace.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.marketplace.entity.EstadoPublicacion;
import com.uade.tpo.marketplace.entity.ItemWishlist;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.entity.Wishlist;
import com.uade.tpo.marketplace.controllers.wishlist.ItemWishlistRequest;
import com.uade.tpo.marketplace.controllers.wishlist.WishlistResponse;
import com.uade.tpo.marketplace.exceptions.AdminNoComerciaException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.ItemWishlistNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.repository.ProductoRepository;
import com.uade.tpo.marketplace.repository.UsuarioRepository;
import com.uade.tpo.marketplace.repository.WishlistRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {
    private final WishlistRepository wishlistRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AutorizacionService autorizacion;

    @Value("${marketplace.wishlist.meses-vigencia:8}")
    private long mesesVigencia;

    /**
     * Pre : el id del usuario y el id de quien pregunta.
     * Post: la wishlist con sus items, cada uno marcado como disponible o no.
     *       La crea vacia si es la primera vez y la vacia si pasaron los 8
     *       meses.
     */
    @Transactional
    public WishlistResponse obtenerWishlist(Long idUsuario)
            throws UsuarioNoEncontradoException, CuentaInactivaException {
        autorizacion.validarActivo(idUsuario);
        return WishlistResponse.from(obtenerEntidad(idUsuario));
    }

    /**
     * Pre : el id del usuario, el request con idProducto y el id de quien
     *       pide.
     * Post: la wishlist con el producto adentro. Si ya estaba no hace nada,
     *       asi que se puede repetir sin duplicar. Tira
     *       ProductoNoEncontradoException si no esta a la venta, y
     *       AdminNoComerciaException si quien pide es ADMIN.
     */
    @Transactional
    public WishlistResponse agregarItem(Long idUsuario, ItemWishlistRequest request)
            throws UsuarioNoEncontradoException,
            ProductoNoEncontradoException, CuentaInactivaException, AdminNoComerciaException {
        autorizacion.validarActivo(idUsuario);

        autorizacion.validarQueNoSeaAdmin(idUsuario);

        Wishlist wishlist = obtenerEntidad(idUsuario);

        Producto producto = productoRepository.findById(request.getIdProducto())
                .filter(Producto::getActivo)
                .filter(p -> p.getEstadoPublicacion() == EstadoPublicacion.PUBLICADO)
                .orElseThrow(ProductoNoEncontradoException::new);

        boolean yaEstaba = wishlist.getItems().stream()
                .anyMatch(i -> i.getProducto().getId().equals(producto.getId()));

        if (!yaEstaba) {
            ItemWishlist item = new ItemWishlist();
            item.setWishlist(wishlist);
            item.setProducto(producto);
            item.setFechaAgregado(LocalDateTime.now());
            wishlist.getItems().add(item);
        }

        renovarVigencia(wishlist);
        return WishlistResponse.from(wishlistRepository.save(wishlist));
    }

    /**
     * Pre : el id del usuario, el id del item y el id de quien pide.
     * Post: la wishlist sin ese item. Tira ItemWishlistNoEncontradoException
     *       si no estaba.
     */
    @Transactional
    public WishlistResponse eliminarItem(Long idUsuario, Long idItem)
            throws UsuarioNoEncontradoException,
            ItemWishlistNoEncontradoException, CuentaInactivaException {
        autorizacion.validarActivo(idUsuario);

        Wishlist wishlist = obtenerEntidad(idUsuario);

        ItemWishlist item = wishlist.getItems().stream()
                .filter(i -> i.getId().equals(idItem))
                .findFirst()
                .orElseThrow(ItemWishlistNoEncontradoException::new);

        wishlist.getItems().remove(item);
        renovarVigencia(wishlist);
        return WishlistResponse.from(wishlistRepository.save(wishlist));
    }

    /**
     * Pre : el id del usuario y el id de quien pide.
     * Post: la wishlist sin items. La lista en si no se borra.
     */
    @Transactional
    public WishlistResponse vaciar(Long idUsuario)
            throws UsuarioNoEncontradoException, CuentaInactivaException {
        autorizacion.validarActivo(idUsuario);

        Wishlist wishlist = obtenerEntidad(idUsuario);
        wishlist.getItems().clear();
        renovarVigencia(wishlist);
        return WishlistResponse.from(wishlistRepository.save(wishlist));
    }

    /**
     * Pre : el id del usuario.
     * Post: la entidad Wishlist, creada si es la primera vez y vaciada si
     *       vencio.
     */
    private Wishlist obtenerEntidad(Long idUsuario) throws UsuarioNoEncontradoException {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);

        Wishlist wishlist = wishlistRepository.findByUsuarioId(idUsuario)
                .orElseGet(() -> crearVacia(usuario));

        return vaciarSiVencio(wishlist);
    }

    /**
     * Pre : el usuario.
     * Post: una wishlist nueva ya guardada. Hay una sola por persona.
     */
    private Wishlist crearVacia(Usuario usuario) {
        Wishlist wishlist = new Wishlist();
        wishlist.setUsuario(usuario);
        return wishlistRepository.save(wishlist);
    }

    /**
     * Pre : la wishlist.
     * Post: la misma, vacia si su fechaLimite ya paso. Mismo vencimiento
     *       perezoso que el carrito.
     */
    private Wishlist vaciarSiVencio(Wishlist wishlist) {
        if (wishlist.getFechaLimite() == null
                || wishlist.getFechaLimite().isAfter(LocalDateTime.now()))
            return wishlist;

        wishlist.getItems().clear();
        wishlist.setFechaLimite(null);
        return wishlistRepository.save(wishlist);
    }

    private void renovarVigencia(Wishlist wishlist) {
        wishlist.setFechaLimite(wishlist.getItems().isEmpty()
                ? null
                : LocalDateTime.now().plusMonths(mesesVigencia));
    }
}
