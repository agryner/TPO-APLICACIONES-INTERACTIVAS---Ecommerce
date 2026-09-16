package com.uade.tpo.marketplace.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.marketplace.entity.EstadoOferta;
import com.uade.tpo.marketplace.entity.Oferta;

@Repository
public interface OfertaRepository extends JpaRepository<Oferta, Long> {
    List<Oferta> findByCompradorIdOrderByFechaCreacionDesc(Long idComprador);

    List<Oferta> findByProductoVendedorIdOrderByFechaCreacionDesc(Long idVendedor);

    Optional<Oferta> findByCompradorIdAndProductoIdAndEstado(Long idComprador, Long idProducto,
            EstadoOferta estado);

    List<Oferta> findByEstadoAndFechaVencimientoBefore(EstadoOferta estado, LocalDateTime momento);
}
