package com.uade.tpo.demo.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.uade.tpo.demo.entity.Categoria;
import com.uade.tpo.demo.exceptions.CategoryDuplicateException;
import com.uade.tpo.demo.repository.CategoryRepository;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public Page<Categoria> getCategories(PageRequest pageable) {
        return categoryRepository.findAll(pageable);
    }

    public Optional<Categoria> getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId);
    }

    public Categoria createCategory(String nombre, String descripcion) throws CategoryDuplicateException {
        List<Categoria> categories = categoryRepository.findByNombre(nombre);
        if (categories.isEmpty()) {
            return categoryRepository.save(new Categoria(nombre, descripcion));
        }
        throw new CategoryDuplicateException();
    }
}