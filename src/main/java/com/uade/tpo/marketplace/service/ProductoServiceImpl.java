package com.uade.tpo.marketplace.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.uade.tpo.marketplace.controllers.ProductoRequest;
import com.uade.tpo.marketplace.entity.Categoria;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.CategoriaNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.repository.CategoriaRepository;
import com.uade.tpo.marketplace.repository.ProductoRepository;
import com.uade.tpo.marketplace.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Logica de productos: altas, ediciones y busquedas.
 */
@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public List<Producto> getProductos(Long idCategoria, Long idVendedor, String nombre, BigDecimal precioMin, BigDecimal precioMax) {
        return productoRepository.findAll().stream()
                .filter(p -> idCategoria == null || (p.getCategoria() != null && p.getCategoria().getId().equals(idCategoria)))
                .filter(p -> idVendedor == null || (p.getVendedor() != null && p.getVendedor().getId().equals(idVendedor)))
                .filter(p -> nombre == null || p.getNombre().toLowerCase().contains(nombre.toLowerCase()))
                .filter(p -> precioMin == null || (p.getPrecio() != null && p.getPrecio().compareTo(precioMin) >= 0))
                .filter(p -> precioMax == null || (p.getPrecio() != null && p.getPrecio().compareTo(precioMax) <= 0))
                .collect(Collectors.toList());
    }

    public Optional<Producto> getProductoById(Long idProducto) {
        return productoRepository.findById(idProducto);
    }

    public Producto createProducto(ProductoRequest request)
            throws CategoriaNoEncontradaException, UsuarioNoEncontradoException {
        Producto producto = new Producto();
        copiarDatos(producto, request);
        return productoRepository.save(producto);
    }

    public Producto updateProducto(Long idProducto, ProductoRequest request)
            throws ProductoNoEncontradoException, CategoriaNoEncontradaException, UsuarioNoEncontradoException {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(ProductoNoEncontradoException::new);

        copiarDatos(producto, request);
        return productoRepository.save(producto);
    }

    public void deleteProducto(Long idProducto) throws ProductoNoEncontradoException {
        if (!productoRepository.existsById(idProducto))
            throw new ProductoNoEncontradoException();

        productoRepository.deleteById(idProducto);
    }

    private void copiarDatos(Producto producto, ProductoRequest request)
            throws CategoriaNoEncontradaException, UsuarioNoEncontradoException {
        Categoria categoria = categoriaRepository.findById(request.getIdCategoria())
                .orElseThrow(CategoriaNoEncontradaException::new);
        Usuario vendedor = usuarioRepository.findById(request.getIdVendedor())
                .orElseThrow(UsuarioNoEncontradoException::new);

        producto.setNombre(request.getNombre());
        producto.setPrecio(request.getPrecio());
        producto.setStock(request.getStock());
        producto.setDescripcion(request.getDescripcion());
        producto.setUbicacion(request.getUbicacion());
        producto.setDescuento(request.getDescuento() == null ? 0 : request.getDescuento());
        producto.setCategoria(categoria);
        producto.setVendedor(vendedor);
    }
}