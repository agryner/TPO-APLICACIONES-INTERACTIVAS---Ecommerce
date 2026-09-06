package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.wishlist.ItemWishlistRequest;
import com.uade.tpo.marketplace.controllers.wishlist.WishlistResponse;
import com.uade.tpo.marketplace.exceptions.AdminNoComerciaException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.ItemWishlistNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;

/**
 * Contrato de la logica de la wishlist.
 *
 * Lo consume WishlistsController y lo implementa WishlistServiceImpl.
 */
public interface WishlistService {

    /** La wishlist del usuario, creada vacia si es la primera vez. */
    WishlistResponse obtenerWishlist(Long idUsuario)
            throws UsuarioNoEncontradoException, CuentaInactivaException;

    /**
     * Guarda un producto para mas adelante. Si ya estaba, no hace nada: la
     * wishlist no tiene cantidades, asi que la operacion es idempotente.
     */
    WishlistResponse agregarItem(Long idUsuario, ItemWishlistRequest request)
            throws UsuarioNoEncontradoException,
            ProductoNoEncontradoException, CuentaInactivaException, AdminNoComerciaException;

    WishlistResponse eliminarItem(Long idUsuario, Long idItem)
            throws UsuarioNoEncontradoException,
            ItemWishlistNoEncontradoException, CuentaInactivaException;

    WishlistResponse vaciar(Long idUsuario)
            throws UsuarioNoEncontradoException, CuentaInactivaException;
}
