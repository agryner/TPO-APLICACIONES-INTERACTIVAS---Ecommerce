package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.entity.dto.ProductoRequest;
import com.uade.tpo.marketplace.entity.EstadoPublicacion;
import com.uade.tpo.marketplace.entity.dto.ProductoCreadoResponse;
import com.uade.tpo.marketplace.entity.dto.ProductoResponse;
import java.math.BigDecimal;
import java.util.List;

import com.uade.tpo.marketplace.exceptions.CategoriaNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.OrdenamientoInvalidoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.TransicionInvalidaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;

/**
 * Contrato de la logica de productos.
 *
 * Lo consume ProductosController y lo implementa ProductoServiceImpl.
 */
public interface ProductoService {

    /**
     * Busqueda con filtros opcionales. Los que llegan en null no filtran, y los
     * que llegan con valor se combinan entre si. vendedor busca por nombre de
     * usuario, por coincidencia parcial y sin distinguir mayusculas, igual que
     * nombre. ordenPrecio acepta "asc" o "desc"; en null se respeta el orden
     * de la base.
     */
    List<ProductoResponse> getProductos(Long idCategoria, String vendedor, String nombre,
            BigDecimal precioMin, BigDecimal precioMax, String ordenPrecio)
            throws OrdenamientoInvalidoException;

    ProductoResponse getProductoById(Long idProducto) throws ProductoNoEncontradoException;

    /**
     * Las publicaciones del solicitante, en cualquier estado.
     *
     * El catalogo publico solo muestra las PUBLICADAS, asi que sin esto un
     * vendedor no tendria como ver sus borradores ni sus pausadas. Con estado
     * en null vienen todas.
     */
    List<ProductoResponse> getMisPublicaciones(Long idSolicitante, EstadoPublicacion estado)
            throws UsuarioNoEncontradoException;

    /**
     * Da de alta a nombre del solicitante: no se puede publicar por otro. El
     * producto queda en BORRADOR hasta que se le suba la primera foto.
     */
    ProductoCreadoResponse createProducto(ProductoRequest request, Long idSolicitante)
            throws CategoriaNoEncontradaException, UsuarioNoEncontradoException, CuentaInactivaException;

    /** Solo el vendedor duenio de la publicacion puede editarla. */
    ProductoResponse updateProducto(Long idProducto, ProductoRequest request, Long idSolicitante)
            throws ProductoNoEncontradoException, CategoriaNoEncontradaException,
            UsuarioNoEncontradoException, OperacionAjenaException, CuentaInactivaException;

    /**
     * Pausa o reanuda una publicacion. Solo el vendedor duenio.
     *
     * PUBLICADO pasa a PAUSADO y al reves. Un BORRADOR no se puede pausar
     * porque todavia no esta en el catalogo, y a BORRADOR no se vuelve a mano:
     * eso pasa solo cuando el producto se queda sin fotos.
     */
    ProductoResponse cambiarEstadoPublicacion(Long idProducto, EstadoPublicacion estado,
            Long idSolicitante)
            throws ProductoNoEncontradoException, OperacionAjenaException,
            TransicionInvalidaException, CuentaInactivaException, UsuarioNoEncontradoException;

    /** Solo el vendedor duenio de la publicacion puede darla de baja. */
    void deleteProducto(Long idProducto, Long idSolicitante)
            throws ProductoNoEncontradoException, OperacionAjenaException, CuentaInactivaException, UsuarioNoEncontradoException;
}
