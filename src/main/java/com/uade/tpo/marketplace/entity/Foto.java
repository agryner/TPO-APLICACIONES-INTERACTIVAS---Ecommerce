package com.uade.tpo.marketplace.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Una foto de producto. El archivo se guarda en la propia tabla, como byte[]
 * en vez de java.sql.Blob: Hibernate lo mapea igual a un LONGBLOB y evita
 * tener que envolver los bytes en un SerialBlob para escribirlos.
 *
 * Persistida por FotoRepository. Producto la trae en cascade ALL, asi
 * que borrar el producto se lleva tambien sus fotos y sus archivos.
 */
@Data
@NoArgsConstructor
@Entity
public class Foto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_foto")
    private Long id;

    /**
     * JsonIgnore para que las respuestas no arrastren la imagen entera: quien
     * quiera los bytes los pide en /fotos/{id}/contenido o /fotos/{id}/base64.
     */
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

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;
}
