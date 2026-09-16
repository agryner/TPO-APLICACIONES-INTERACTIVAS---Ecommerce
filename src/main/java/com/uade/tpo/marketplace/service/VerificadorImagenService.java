package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.entity.Categoria;

public interface VerificadorImagenService {
    record Resultado(boolean coincide, double confianza, String queVeo,
            String categoriaSugerida, String mensajeAlVendedor) {
        public double puntaje() {
            return coincide ? confianza : 1 - confianza;
        }
    }

    Resultado verificar(byte[] imagen, Categoria categoria) throws Exception;
}
