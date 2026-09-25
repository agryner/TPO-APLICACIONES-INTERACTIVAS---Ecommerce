package com.uade.tpo.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class DireccionEntrega {
    @Enumerated(EnumType.STRING)
    @Column(name = "entrega_provincia")
    private Provincia provincia;

    @Column(name = "entrega_localidad", length = 100)
    private String localidad;

    @Column(name = "entrega_calle", length = 150)
    private String calle;

    /**
     * Pre : nada.
     * Post: si estan las tres partes. Una entrega a coordinar no lleva ninguna,
     *       asi que vacia es un estado valido y no un error.
     */
    public boolean estaCompleta() {
        return provincia != null
                && localidad != null && !localidad.isBlank()
                && calle != null && !calle.isBlank();
    }
}
