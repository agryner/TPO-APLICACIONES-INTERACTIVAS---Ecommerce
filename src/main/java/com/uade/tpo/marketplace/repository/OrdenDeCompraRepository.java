package com.uade.tpo.marketplace.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.marketplace.entity.OrdenDeCompra;

@Repository
public interface OrdenDeCompraRepository extends JpaRepository<OrdenDeCompra, Long> {
    List<OrdenDeCompra> findByCompradorId(Long idComprador);

    List<OrdenDeCompra> findByVendedorId(Long idVendedor);

    List<OrdenDeCompra> findByCompradorIdOrVendedorId(Long idComprador, Long idVendedor);
}
