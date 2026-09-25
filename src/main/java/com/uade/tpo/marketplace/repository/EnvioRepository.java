package com.uade.tpo.marketplace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.marketplace.entity.Envio;
import com.uade.tpo.marketplace.entity.EstadoEnvio;

@Repository
public interface EnvioRepository extends JpaRepository<Envio, Long> {
    Optional<Envio> findByNumeroSeguimiento(String numeroSeguimiento);

    boolean existsByNumeroSeguimiento(String numeroSeguimiento);

    List<Envio> findByDespachanteIdAndEstadoOrderByFechaDespachoAsc(Long idDespachante,
            EstadoEnvio estado);

    List<Envio> findByDespachanteIdAndEstadoOrderByFechaEntregaDesc(Long idDespachante,
            EstadoEnvio estado);

    Optional<Envio> findByOrdenId(Long idOrden);

    List<Envio> findByOrdenCompradorIdOrderByFechaCreacionDesc(Long idComprador);

    List<Envio> findByOrdenVendedorIdOrderByFechaCreacionDesc(Long idVendedor);
}
