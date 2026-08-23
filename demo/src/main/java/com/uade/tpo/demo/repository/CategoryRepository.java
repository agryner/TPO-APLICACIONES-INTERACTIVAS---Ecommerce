package com.uade.tpo.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.uade.tpo.demo.entity.Categoria;

@Repository
public interface CategoryRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByNombre(String nombre);
}
