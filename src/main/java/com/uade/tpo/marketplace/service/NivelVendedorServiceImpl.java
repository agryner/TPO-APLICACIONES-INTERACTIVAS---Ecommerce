package com.uade.tpo.marketplace.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.uade.tpo.marketplace.entity.NivelVendedor;
import com.uade.tpo.marketplace.repository.ResenaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NivelVendedorServiceImpl implements NivelVendedorService {
    private final ResenaRepository resenaRepository;

    @Value("${marketplace.vendedor.oro.promedio:4.0}")
    private double promedioOro;

    @Value("${marketplace.vendedor.oro.resenas:5}")
    private long resenasOro;

    @Value("${marketplace.vendedor.platino.promedio:4.5}")
    private double promedioPlatino;

    @Value("${marketplace.vendedor.platino.resenas:10}")
    private long resenasPlatino;

    /**
     * Pre : el promedio de sus resenas y cuantas son.
     * Post: el nivel que le toca. Cada escalon pide promedio Y cantidad: con el
     *       promedio solo, una unica resena de 5 estrellas te haria PLATINO, que
     *       es justo lo que el comprador no tiene que creer. Cero resenas cae en
     *       SIN_CALIFICAR y no en el escalon mas bajo, porque no haber vendido
     *       todavia no es lo mismo que haber vendido mal.
     */
    public NivelVendedor calcular(Double promedio, long cantidad) {
        if (promedio == null || cantidad == 0)
            return NivelVendedor.SIN_CALIFICAR;

        if (promedio >= promedioPlatino && cantidad >= resenasPlatino)
            return NivelVendedor.PLATINO;

        if (promedio >= promedioOro && cantidad >= resenasOro)
            return NivelVendedor.ORO;

        return NivelVendedor.BRONCE;
    }

    /**
     * Pre : nada.
     * Post: el nivel de cada vendedor que tenga al menos una resena, de UNA
     *       sola consulta. El catalogo lo necesita para todos los productos de
     *       la pagina; pedirlo de a uno seria una consulta por producto.
     */
    public Map<Long, NivelVendedor> deTodos() {
        Map<Long, NivelVendedor> niveles = new HashMap<>();

        for (Object[] fila : resenaRepository.resumenPorVendedor()) {
            Long idVendedor = (Long) fila[0];
            Double promedio = fila[1] == null ? null : ((Number) fila[1]).doubleValue();
            long cantidad = ((Number) fila[2]).longValue();
            niveles.put(idVendedor, calcular(promedio, cantidad));
        }

        return niveles;
    }
}
