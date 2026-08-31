package com.uade.tpo.marketplace.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.marketplace.entity.Categoria;

/**
 * Acceso a datos de Categoria.
 *
 * Spring Data genera la implementacion a partir de los nombres de los
 * metodos. Lo usa CategoriaServiceImpl; los controllers nunca lo tocan.
 */
@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByCategoriaPadreId(Long idCategoriaPadre);

    List<Categoria> findByCategoriaPadreIsNull();

    // El nombre solo tiene que ser unico entre hermanas: puede haber una
    // "Hibridos" colgando de Semillas y otra colgando de Forrajes.
    List<Categoria> findByNombreAndCategoriaPadreId(String nombre, Long idCategoriaPadre);

    List<Categoria> findByNombreAndCategoriaPadreIsNull(String nombre);
}
