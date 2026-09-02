package com.uade.tpo.marketplace.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.marketplace.entity.OrdenDeCompra;

/**
 * Acceso a datos de OrdenDeCompra.
 *
 * Lo usa OrdenDeCompraServiceImpl para listar tanto las compras como las
 * ventas de un usuario, que son la misma orden vista desde cada punta.
 */
@Repository
public interface OrdenDeCompraRepository extends JpaRepository<OrdenDeCompra, Long> {

    /** Historial de compras de un usuario. */
    List<OrdenDeCompra> findByCompradorId(Long idComprador);

    /** Ventas de un usuario: las ordenes en las que aparece del otro lado. */
    List<OrdenDeCompra> findByVendedorId(Long idVendedor);

    /**
     * Las ordenes en las que el usuario participa, de cualquiera de los dos
     * lados. Es lo que hay detras del listado por defecto: nadie ve ordenes
     * ajenas, asi que ningun endpoint termina en un findAll suelto.
     */
    List<OrdenDeCompra> findByCompradorIdOrVendedorId(Long idComprador, Long idVendedor);
}
