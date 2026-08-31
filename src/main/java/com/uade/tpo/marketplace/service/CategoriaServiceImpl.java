package com.uade.tpo.marketplace.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.uade.tpo.marketplace.entity.Categoria;
import com.uade.tpo.marketplace.entity.TipoUsuario;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.controllers.CategoriaRequest;
import com.uade.tpo.marketplace.exceptions.CategoriaConSubcategoriasException;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.CategoriaDuplicadaException;
import com.uade.tpo.marketplace.exceptions.CategoriaNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.JerarquiaInvalidaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.repository.CategoriaRepository;
import com.uade.tpo.marketplace.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Logica de categorias: altas, jerarquia y validaciones.
 *
 * Lo llama CategoriasController y se apoya en CategoriaRepository. Valida que
 * no haya dos hermanas con el mismo nombre, que una categoria no quede como
 * descendiente de si misma y que no se borre una que tenga subcategorias.
 * Consulta ademas UsuarioRepository, porque solo un ADMIN puede crear,
 * editar o borrar categorias.
 */
@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public List<Categoria> getCategorias() {
        return categoriaRepository.findAll();
    }

    public List<Categoria> getCategoriasRaiz() {
        return categoriaRepository.findByCategoriaPadreIsNull();
    }

    public List<Categoria> getSubcategorias(Long idCategoria) throws CategoriaNoEncontradaException {
        if (!categoriaRepository.existsById(idCategoria))
            throw new CategoriaNoEncontradaException();

        return categoriaRepository.findByCategoriaPadreId(idCategoria);
    }

    public Optional<Categoria> getCategoriaById(Long idCategoria) {
        return categoriaRepository.findById(idCategoria);
    }

    public Categoria createCategoria(CategoriaRequest request, Long idUsuario)
            throws CategoriaDuplicadaException, CategoriaNoEncontradaException,
            UsuarioNoEncontradoException, AccesoDenegadoException {
        validarAdmin(idUsuario);

        Categoria padre = buscarPadre(request.getIdCategoriaPadre());

        if (!hermanasConEseNombre(request.getNombre(), padre).isEmpty())
            throw new CategoriaDuplicadaException();

        Categoria categoria = new Categoria(request.getNombre(), request.getDescripcion());
        categoria.setCategoriaPadre(padre);
        return categoriaRepository.save(categoria);
    }

    public Categoria updateCategoria(Long idCategoria, CategoriaRequest request, Long idUsuario)
            throws CategoriaNoEncontradaException, JerarquiaInvalidaException,
            UsuarioNoEncontradoException, AccesoDenegadoException {
        validarAdmin(idUsuario);

        Categoria categoria = categoriaRepository.findById(idCategoria)
                .orElseThrow(CategoriaNoEncontradaException::new);

        Categoria padre = buscarPadre(request.getIdCategoriaPadre());
        validarJerarquia(categoria, padre);

        categoria.setNombre(request.getNombre());
        categoria.setDescripcion(request.getDescripcion());
        categoria.setCategoriaPadre(padre);
        return categoriaRepository.save(categoria);
    }

    public void deleteCategoria(Long idCategoria, Long idUsuario)
            throws CategoriaNoEncontradaException, CategoriaConSubcategoriasException,
            UsuarioNoEncontradoException, AccesoDenegadoException {
        validarAdmin(idUsuario);

        if (!categoriaRepository.existsById(idCategoria))
            throw new CategoriaNoEncontradaException();

        if (!categoriaRepository.findByCategoriaPadreId(idCategoria).isEmpty())
            throw new CategoriaConSubcategoriasException();

        categoriaRepository.deleteById(idCategoria);
    }

    private Categoria buscarPadre(Long idCategoriaPadre) throws CategoriaNoEncontradaException {
        if (idCategoriaPadre == null)
            return null;

        return categoriaRepository.findById(idCategoriaPadre)
                .orElseThrow(CategoriaNoEncontradaException::new);
    }

    private List<Categoria> hermanasConEseNombre(String nombre, Categoria padre) {
        return padre == null
                ? categoriaRepository.findByNombreAndCategoriaPadreIsNull(nombre)
                : categoriaRepository.findByNombreAndCategoriaPadreId(nombre, padre.getId());
    }

    /**
     * Impide que una categoria termine siendo antepasado de si misma, que
     * dejaria la jerarquia en un ciclo infinito.
     */
    private void validarJerarquia(Categoria categoria, Categoria nuevoPadre)
            throws JerarquiaInvalidaException {
        Categoria actual = nuevoPadre;
        while (actual != null) {
            if (actual.getId().equals(categoria.getId()))
                throw new JerarquiaInvalidaException();

            actual = actual.getCategoriaPadre();
        }
    }

    /**
     * Corta la operacion si el usuario que la pide no es ADMIN.
     *
     * Mientras no haya autenticacion, el id llega como parametro desde el
     * controller. Cuando se sume el token, el idUsuario sale de ahi y este
     * metodo no cambia.
     */
    private void validarAdmin(Long idUsuario)
            throws UsuarioNoEncontradoException, AccesoDenegadoException {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);

        if (usuario.getRol() != TipoUsuario.ADMIN)
            throw new AccesoDenegadoException();
    }

}
