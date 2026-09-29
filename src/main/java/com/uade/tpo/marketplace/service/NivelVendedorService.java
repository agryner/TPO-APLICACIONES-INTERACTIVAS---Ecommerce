package com.uade.tpo.marketplace.service;

import java.util.Map;

import com.uade.tpo.marketplace.entity.NivelVendedor;

public interface NivelVendedorService {
    NivelVendedor calcular(Double promedio, long cantidad);

    Map<Long, NivelVendedor> deTodos();
}
