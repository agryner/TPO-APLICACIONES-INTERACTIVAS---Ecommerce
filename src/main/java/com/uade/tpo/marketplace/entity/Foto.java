package com.uade.tpo.marketplace.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@Entity
public class Foto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_foto")
    private Long id;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] contenido;

    @Column(name = "nombre_archivo")
    private String nombreArchivo;

    @Column(name = "tipo_contenido")
    private String tipoContenido;

    @Column
    private Long tamanio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_verificacion", nullable = false)
    private EstadoVerificacion estadoVerificacion = EstadoVerificacion.EN_REVISION;

    @Column(name = "confianza_ia")
    private Double confianzaIa;

    @Column(name = "que_ve_ia")
    private String queVeIa;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;
}
