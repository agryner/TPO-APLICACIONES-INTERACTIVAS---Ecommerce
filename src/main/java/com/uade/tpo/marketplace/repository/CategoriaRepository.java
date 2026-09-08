package com.uade.tpo.marketplace.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.marketplace.entity.Categoria;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByCategoriaPadreId(Long idCategoriaPadre);

    List<Categoria> findByCategoriaPadreIsNull();

    List<Categoria> findByNombreAndCategoriaPadreId(String nombre, Long idCategoriaPadre);

    List<Categoria> findByNombreAndCategoriaPadreIsNull(String nombre);
}
