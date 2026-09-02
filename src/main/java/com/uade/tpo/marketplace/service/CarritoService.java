package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.entity.dto.CarritoResponse;
import com.uade.tpo.marketplace.entity.dto.ItemCarritoRequest;
import com.uade.tpo.marketplace.entity.Carrito;
import com.uade.tpo.marketplace.exceptions.ItemCarritoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.StockInsuficienteException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;

/**
 * Contrato de la logica del carrito.
 *
 * Lo consume CarritosController y lo implementa CarritoServiceImpl.
 */
public interface CarritoService {

    /**
     * Devuelve el carrito del usuario. Si nunca tuvo uno, se lo crea vacio.
     * Si el carrito venció, se devuelve vacio.
     */
    CarritoResponse obtenerCarrito(Long idUsuario, Long idSolicitante) throws OperacionAjenaException, UsuarioNoEncontradoException;

    /**
     * Version para uso entre services: devuelve la entidad en vez del DTO,
     * porque OrdenDeCompraServiceImpl necesita recorrer los items y tocar el
     * stock. La capa de trafico usa obtenerCarrito.
     */
    Carrito obtenerCarritoEntidad(Long idUsuario) throws UsuarioNoEncontradoException;

    /**
     * Saca un producto de todos los carritos donde este cargado.
     *
     * La llaman los services cuando el producto deja de estar disponible: se
     * pausa, se da de baja, o se queda sin fotos y vuelve a borrador. Sin esto
     * el comprador se entera recien al intentar pagar.
     */
    void quitarDeTodosLosCarritos(Long idProducto);

    /**
     * Version para uso entre services: vacia sin validar pertenencia, porque
     * quien llama ya verifico de quien es el carrito.
     */
    void vaciarEntidad(Long idUsuario) throws UsuarioNoEncontradoException;

    CarritoResponse agregarItem(Long idUsuario, ItemCarritoRequest request, Long idSolicitante)
            throws OperacionAjenaException, UsuarioNoEncontradoException, ProductoNoEncontradoException, StockInsuficienteException;

    /**
     * Cambia la cantidad de un item ya cargado. Una cantidad nula o menor o
     * igual a cero equivale a sacar el item del carrito.
     */
    CarritoResponse modificarCantidad(Long idUsuario, Long idItem, Integer nuevaCantidad,
            Long idSolicitante)
            throws OperacionAjenaException, UsuarioNoEncontradoException, ItemCarritoNoEncontradoException,
            StockInsuficienteException;

    CarritoResponse eliminarItem(Long idUsuario, Long idItem, Long idSolicitante)
            throws OperacionAjenaException, UsuarioNoEncontradoException, ItemCarritoNoEncontradoException;

    CarritoResponse vaciar(Long idUsuario, Long idSolicitante) throws OperacionAjenaException, UsuarioNoEncontradoException;
}
