package com.uade.tpo.marketplace.controllers.productos;

import java.math.BigDecimal;
import java.util.List;

import com.uade.tpo.marketplace.entity.EstadoPublicacion;
import java.time.LocalDateTime;
import com.uade.tpo.marketplace.entity.CondicionProducto;
import com.uade.tpo.marketplace.entity.EstadoVerificacion;
import com.uade.tpo.marketplace.entity.NivelDestacado;
import com.uade.tpo.marketplace.entity.Provincia;
import com.uade.tpo.marketplace.entity.Producto;

import lombok.Data;
import com.uade.tpo.marketplace.controllers.categorias.CategoriaResponse;
import com.uade.tpo.marketplace.controllers.fotos.FotoResponse;
import com.uade.tpo.marketplace.controllers.resenas.ResenaResponse;

@Data
public class ProductoResponse {
    private Long id;
    private String nombre;
    private BigDecimal precio;
    private BigDecimal precioFinal;
    private Double calificacion;
    private long cantidadResenas;
    private Integer stock;
    private Integer vendidos;
    private String descripcion;
    private String ubicacion;
    private Integer descuento;
    private CategoriaResponse categoria;
    private VendedorResponse vendedor;
    private Boolean admiteEnvio;
    private Boolean aceptaOfertas;
    private Provincia provincia;
    private CondicionProducto condicion;
    private Integer anio;
    private NivelDestacado nivelDestacado;
    private LocalDateTime destacadoHasta;
    private Boolean activo;

    private EstadoPublicacion estadoPublicacion;

    private List<FotoResponse> fotos;
    private List<ResenaResponse> ultimasResenas;

    /**
     * Pre : la entidad Producto, o null.
     * Post: el producto con su categoria y sus fotos ya aplanados. El
     *       vendedor, la calificacion y las ultimas resenas los completa el
     *       service: necesitan consultas que un DTO no tiene por que hacer.
     */
    public static ProductoResponse from(Producto producto) {
        if (producto == null)
            return null;

        ProductoResponse dto = new ProductoResponse();
        dto.setId(producto.getId());
        dto.setNombre(producto.getNombre());
        dto.setPrecio(producto.getPrecio());
        dto.setStock(producto.getStock());
        dto.setVendidos(producto.getVendidos());
        dto.setDescripcion(producto.getDescripcion());
        dto.setUbicacion(producto.getUbicacion());
        dto.setDescuento(producto.getDescuento());
        dto.setAdmiteEnvio(producto.getAdmiteEnvio());
        dto.setAceptaOfertas(producto.getAceptaOfertas());
        dto.setProvincia(producto.getProvincia());
        dto.setCondicion(producto.getCondicion());
        dto.setAnio(producto.getAnio());
        dto.setNivelDestacado(producto.nivelVigente());
        dto.setDestacadoHasta(producto.getDestacadoHasta());
        dto.setActivo(producto.getActivo());
        dto.setEstadoPublicacion(producto.getEstadoPublicacion());
        dto.setCategoria(CategoriaResponse.from(producto.getCategoria()));
        dto.setPrecioFinal(ProductoResumenResponse.conDescuento(producto));
        // Sin nivel ni calificacion: eso pide consultas y lo completa el
        // service en el detalle. Al publicar o editar no hace falta.
        dto.setVendedor(VendedorResponse.from(producto.getVendedor(), null, null, 0));
        // Solo las aprobadas: este DTO es la vista publica del producto. Las
        // que quedaron sin revisar las ve su dueño en GET /fotos?idProducto=.
        dto.setFotos(producto.getFotos() == null ? List.of()
                : producto.getFotos().stream()
                        .filter(f -> Boolean.TRUE.equals(f.getActivo()))
                        .filter(f -> f.getEstadoVerificacion() == EstadoVerificacion.APROBADA)
                        .map(FotoResponse::from)
                        .toList());
        return dto;
    }
}
