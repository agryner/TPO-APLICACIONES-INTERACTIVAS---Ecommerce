package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.wishlist.ItemWishlistRequest;
import com.uade.tpo.marketplace.controllers.wishlist.WishlistResponse;
import com.uade.tpo.marketplace.exceptions.AdminNoComerciaException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.ItemWishlistNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;

public interface WishlistService {
    WishlistResponse obtenerWishlist(Long idUsuario)
            throws UsuarioNoEncontradoException, CuentaInactivaException;

    WishlistResponse agregarItem(Long idUsuario, ItemWishlistRequest request)
            throws UsuarioNoEncontradoException,
            ProductoNoEncontradoException, CuentaInactivaException, AdminNoComerciaException;

    WishlistResponse eliminarItem(Long idUsuario, Long idItem)
            throws UsuarioNoEncontradoException,
            ItemWishlistNoEncontradoException, CuentaInactivaException;

    WishlistResponse vaciar(Long idUsuario)
            throws UsuarioNoEncontradoException, CuentaInactivaException;
}
