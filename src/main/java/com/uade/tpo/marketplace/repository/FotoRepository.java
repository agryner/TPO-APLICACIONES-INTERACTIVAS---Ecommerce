package com.uade.tpo.marketplace.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.marketplace.entity.Foto;

/**
 * Acceso a datos de Foto, incluido el binario de la imagen.
 *
 * Lo usa FotoServiceImpl. findByProductoId es el que alimenta el listado de
 * fotos de un producto.
 */
@Repository
public interface FotoRepository extends JpaRepository<Foto, Long> {

    List<Foto> findByProductoId(Long idProducto);
}
