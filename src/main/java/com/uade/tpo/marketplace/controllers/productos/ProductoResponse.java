package com.uade.tpo.marketplace.controllers.productos;

import java.math.BigDecimal;
import java.util.List;

import com.uade.tpo.marketplace.entity.EstadoPublicacion;
import com.uade.tpo.marketplace.entity.Producto;

import lombok.Data;
import com.uade.tpo.marketplace.controllers.categorias.CategoriaResponse;
import com.uade.tpo.marketplace.controllers.fotos.FotoResponse;
import com.uade.tpo.marketplace.controllers.usuarios.UsuarioPublicoResponse;

@Data
public class ProductoResponse {
    private Long id;
    private String nombre;
    private BigDecimal precio;
    private Integer stock;
    private Integer vendidos;
    private String descripcion;
    private String ubicacion;
    private Integer descuento;
    private CategoriaResponse categoria;
    private UsuarioPublicoResponse vendedor;
    private Boolean activo;

    private EstadoPublicacion estadoPublicacion;

    private List<FotoResponse> fotos;

    /**
     * Pre : la entidad Producto, o null.
     * Post: el producto con su categoria, su vendedor y sus fotos ya
     *       aplanados, para que el cliente no tenga que encadenar pedidos.
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
        dto.setActivo(producto.getActivo());
        dto.setEstadoPublicacion(producto.getEstadoPublicacion());
        dto.setCategoria(CategoriaResponse.from(producto.getCategoria()));
        dto.setVendedor(UsuarioPublicoResponse.from(producto.getVendedor()));
        dto.setFotos(producto.getFotos() == null ? List.of()
                : producto.getFotos().stream().map(FotoResponse::from).toList());
        return dto;
    }
}
