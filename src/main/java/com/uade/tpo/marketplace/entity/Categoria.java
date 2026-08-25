package com.uade.tpo.marketplace.entity;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Categoria de productos, con jerarquia propia.
 *
 * Una categoria sin padre es una categoria raiz; si tiene padre, es una
 * subcategoria. La profundidad no esta limitada.
 */
@Data
@NoArgsConstructor
@Entity
public class Categoria {

    public Categoria(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column
    private String descripcion;

    /** Null en las categorias raiz. */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "id_categoria_padre")
    private Categoria categoriaPadre;

    // Se consulta por GET /categorias/{id}/subcategorias. No se serializa para
    // no entrar en un ciclo con categoriaPadre.
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "categoriaPadre")
    private List<Categoria> subcategorias = new ArrayList<>();

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "categoria")
    private List<Producto> productos;

    public boolean esRaiz() {
        return categoriaPadre == null;
    }
}
