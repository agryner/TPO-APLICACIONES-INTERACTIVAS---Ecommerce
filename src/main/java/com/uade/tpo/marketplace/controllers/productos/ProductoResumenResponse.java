package com.uade.tpo.marketplace.controllers.productos;

import java.math.BigDecimal;

import com.uade.tpo.marketplace.entity.EstadoPublicacion;
import com.uade.tpo.marketplace.entity.Foto;
import com.uade.tpo.marketplace.entity.CondicionProducto;
import com.uade.tpo.marketplace.entity.NivelDestacado;
import com.uade.tpo.marketplace.entity.Provincia;
import com.uade.tpo.marketplace.entity.Producto;

import lombok.Data;

@Data
public class ProductoResumenResponse {
    private Long id;
    private String nombre;
    private BigDecimal precio;
    private BigDecimal precioFinal;
    private Integer descuento;
    private Provincia provincia;
    private String ubicacion;
    private CondicionProducto condicion;
    private Integer anio;
    private String foto;
    private Integer vistos;
    private Boolean admiteEnvio;
    private Boolean aceptaOfertas;
    private NivelDestacado nivelDestacado;
    private boolean disponible;

    /**
     * Pre : la entidad Producto, o null.
     * Post: lo que necesita una tarjeta de catalogo y nada mas. No lleva la
     *       categoria, el vendedor, la descripcion ni los metadatos de las
     *       fotos: todo eso es de GET /productos/{id}. De las fotos sale una
     *       sola URL, la de la primera activa.
     */
    public static ProductoResumenResponse from(Producto producto) {
        if (producto == null)
            return null;

        ProductoResumenResponse dto = new ProductoResumenResponse();
        dto.setId(producto.getId());
        dto.setNombre(producto.getNombre());
        dto.setPrecio(producto.getPrecio());
        dto.setDescuento(producto.getDescuento() == null ? 0 : producto.getDescuento());
        dto.setPrecioFinal(conDescuento(producto));
        dto.setProvincia(producto.getProvincia());
        dto.setUbicacion(producto.getUbicacion());
        dto.setCondicion(producto.getCondicion());
        dto.setAnio(producto.getAnio());
        dto.setFoto(primeraFoto(producto));
        dto.setVistos(producto.getVistos() == null ? 0 : producto.getVistos());
        dto.setAdmiteEnvio(producto.getAdmiteEnvio());
        dto.setAceptaOfertas(producto.getAceptaOfertas());
        dto.setNivelDestacado(producto.nivelVigente());
        dto.setDisponible(estaDisponible(producto));
        return dto;
    }

    /**
     * Pre : el producto.
     * Post: el precio con el descuento ya aplicado. Se calcula del lado del
     *       servidor a proposito: si viajaran precio y descuento sueltos, cada
     *       cliente tendria que repetir esta cuenta y podrian no coincidir.
     */
    public static BigDecimal conDescuento(Producto producto) {
        BigDecimal precio = producto.getPrecio();
        if (precio == null)
            return null;

        int descuento = producto.getDescuento() == null ? 0 : producto.getDescuento();
        return precio.multiply(BigDecimal.valueOf(100 - descuento))
                .divide(BigDecimal.valueOf(100));
    }

    /**
     * Pre : el producto.
     * Post: si hoy se puede comprar. Vive aca para que el catalogo, el carrito
     *       y la wishlist usen la misma definicion y no tres parecidas.
     */
    public static boolean estaDisponible(Producto producto) {
        return producto != null
                && Boolean.TRUE.equals(producto.getActivo())
                && producto.getEstadoPublicacion() == EstadoPublicacion.PUBLICADO
                && producto.getVendedor() != null
                && Boolean.TRUE.equals(producto.getVendedor().getActivo())
                && producto.getStock() != null && producto.getStock() > 0;
    }

    /**
     * Pre : el producto.
     * Post: la URL de su primera foto activa, o null si no tiene ninguna.
     */
    private static String primeraFoto(Producto producto) {
        if (producto.getFotos() == null)
            return null;

        return producto.getFotos().stream()
                .filter(f -> Boolean.TRUE.equals(f.getActivo()))
                .map(Foto::getId)
                .findFirst()
                .map(id -> "/fotos/" + id + "/contenido")
                .orElse(null);
    }
}
