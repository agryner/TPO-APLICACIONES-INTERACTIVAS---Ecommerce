package com.uade.tpo.marketplace.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.marketplace.entity.Foto;

@Repository
public interface FotoRepository extends JpaRepository<Foto, Long> {

    List<Foto> findByProductoId(Long idProducto);
}
