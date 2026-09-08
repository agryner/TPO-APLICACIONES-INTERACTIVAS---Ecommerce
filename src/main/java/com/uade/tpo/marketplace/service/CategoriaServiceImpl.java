package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.categorias.CategoriaRequest;
import com.uade.tpo.marketplace.controllers.categorias.CategoriaResponse;
import java.util.List;

import org.springframework.stereotype.Service;

import com.uade.tpo.marketplace.entity.Categoria;
import com.uade.tpo.marketplace.exceptions.CategoriaConProductosException;
import com.uade.tpo.marketplace.exceptions.CategoriaConSubcategoriasException;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.CategoriaDuplicadaException;
import com.uade.tpo.marketplace.exceptions.CategoriaNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.JerarquiaInvalidaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.repository.CategoriaRepository;
import com.uade.tpo.marketplace.repository.ProductoRepository;

import lombok.RequiredArgsConstructor;

/**
 * Logica de categorias: altas, jerarquia y validaciones.
 *
 * Lo llama CategoriasController y se apoya en CategoriaRepository. Valida que
 * no haya dos hermanas con el mismo nombre, que una categoria no quede como
 * descendiente de si misma y que no se borre una que tenga subcategorias.
 * Se apoya en AutorizacionService, porque solo un ADMIN puede crear, editar
 * o borrar categorias.
 */
@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final AutorizacionService autorizacion;

    public List<CategoriaResponse> getCategorias() {
        return categoriaRepository.findAll().stream()
                .map(CategoriaResponse::from)
                .toList();
    }

    public List<CategoriaResponse> getCategoriasRaiz() {
        return categoriaRepository.findByCategoriaPadreIsNull().stream()
                .map(CategoriaResponse::from)
                .toList();
    }

    /**
     * Pre : el id del padre.
     * Post: sus hijas inmediatas, sin nietas. Tira
     *       CategoriaNoEncontradaException si el padre no existe.
     */
    public List<CategoriaResponse> getSubcategorias(Long idCategoria) throws CategoriaNoEncontradaException {
        if (!categoriaRepository.existsById(idCategoria))
            throw new CategoriaNoEncontradaException();

        return categoriaRepository.findByCategoriaPadreId(idCategoria).stream()
                .map(CategoriaResponse::from)
                .toList();
    }

    public CategoriaResponse getCategoriaById(Long idCategoria) throws CategoriaNoEncontradaException {
        return categoriaRepository.findById(idCategoria)
                .map(CategoriaResponse::from)
                .orElseThrow(CategoriaNoEncontradaException::new);
    }

    /**
     * Pre : el request y el id de quien pide, que tiene que ser ADMIN.
     * Post: la categoria creada. Tira CategoriaDuplicadaException si ya hay
     *       una hermana con ese nombre, y CategoriaNoEncontradaException si el
     *       padre indicado no existe.
     */
    public CategoriaResponse createCategoria(CategoriaRequest request, Long idSolicitante)
            throws CategoriaDuplicadaException, CategoriaNoEncontradaException,
            UsuarioNoEncontradoException, AccesoDenegadoException {
        autorizacion.validarAdmin(idSolicitante);

        Categoria padre = buscarPadre(request.getIdCategoriaPadre());
        validarNombreLibre(request.getNombre(), padre, null);

        Categoria categoria = new Categoria(request.getNombre(), request.getDescripcion());
        categoria.setCategoriaPadre(padre);
        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    /**
     * Pre : el id, el request y el id de quien pide.
     * Post: la categoria actualizada, movida de lugar si el request traia otro
     *       padre. Tira JerarquiaInvalidaException si el movimiento armaria un
     *       ciclo.
     */
    public CategoriaResponse updateCategoria(Long idCategoria, CategoriaRequest request, Long idSolicitante)
            throws CategoriaNoEncontradaException, JerarquiaInvalidaException,
            CategoriaDuplicadaException, UsuarioNoEncontradoException, AccesoDenegadoException {
        autorizacion.validarAdmin(idSolicitante);

        Categoria categoria = categoriaRepository.findById(idCategoria)
                .orElseThrow(CategoriaNoEncontradaException::new);

        Categoria padre = buscarPadre(request.getIdCategoriaPadre());
        validarJerarquia(categoria, padre);
        validarNombreLibre(request.getNombre(), padre, idCategoria);

        categoria.setNombre(request.getNombre());
        categoria.setDescripcion(request.getDescripcion());
        categoria.setCategoriaPadre(padre);
        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    /**
     * Pre : el id y el id de quien pide.
     * Post: nada. Es borrado real. Tira CategoriaConSubcategoriasException o
     *       CategoriaConProductosException si no esta vacia, porque borrarla
     *       dejaria registros apuntando a la nada.
     */
    public void deleteCategoria(Long idCategoria, Long idSolicitante)
            throws CategoriaNoEncontradaException, CategoriaConSubcategoriasException,
            CategoriaConProductosException, UsuarioNoEncontradoException,
            AccesoDenegadoException {
        autorizacion.validarAdmin(idSolicitante);

        if (!categoriaRepository.existsById(idCategoria))
            throw new CategoriaNoEncontradaException();

        if (!categoriaRepository.findByCategoriaPadreId(idCategoria).isEmpty())
            throw new CategoriaConSubcategoriasException();

        // Sin este control la baja la termina rechazando la foreign key de
        // producto.id_categoria, y eso sale como un 500 con el SQL adentro.
        if (productoRepository.existsByCategoriaId(idCategoria))
            throw new CategoriaConProductosException();

        categoriaRepository.deleteById(idCategoria);
    }

    /**
     * Pre : el id del padre, que puede venir en null.
     * Post: la entidad, o null si no se indico ninguno. Tira
     *       CategoriaNoEncontradaException si el id no existe.
     */
    private Categoria buscarPadre(Long idCategoriaPadre) throws CategoriaNoEncontradaException {
        if (idCategoriaPadre == null)
            return null;

        return categoriaRepository.findById(idCategoriaPadre)
                .orElseThrow(CategoriaNoEncontradaException::new);
    }

    /**
     * Rechaza el nombre si ya esta usado en esa rama del arbol.
     *
     * Son dos colisiones distintas: contra una hermana, y contra el propio
     * padre. La segunda no la detecta la busqueda de hermanas, porque el padre
     * no es hija de si mismo, y dejaba pasar arboles como "Semillas > Semillas".
     *
     * idActual permite excluirse a si misma al editar: renombrar una categoria
     * dejandole el mismo nombre no puede ser un duplicado.
     *
     * Pre : el nombre, el padre y el id de la propia categoria si se esta
     *       editando.
     * Post: nada si el nombre esta libre entre sus hermanas. Tira
     *       CategoriaDuplicadaException si choca. Se excluye a si misma para
     *       que renombrarla sin cambiarle el nombre no falle.
     */
    private void validarNombreLibre(String nombre, Categoria padre, Long idActual)
            throws CategoriaDuplicadaException {
        if (padre != null && padre.getNombre().equalsIgnoreCase(nombre))
            throw new CategoriaDuplicadaException();

        boolean hayHermana = hermanas(padre).stream()
                .filter(c -> idActual == null || !c.getId().equals(idActual))
                .anyMatch(c -> c.getNombre().equalsIgnoreCase(nombre));

        if (hayHermana)
            throw new CategoriaDuplicadaException();
    }

    private List<Categoria> hermanas(Categoria padre) {
        return padre == null
                ? categoriaRepository.findByCategoriaPadreIsNull()
                : categoriaRepository.findByCategoriaPadreId(padre.getId());
    }

    /**
     * Impide que una categoria termine siendo antepasado de si misma, que
     * dejaria la jerarquia en un ciclo infinito.
     *
     * Pre : la categoria y el padre nuevo.
     * Post: nada si el movimiento es legal. Tira JerarquiaInvalidaException si
     *       el padre nuevo es la propia categoria o una de sus descendientes:
     *       recorre toda la cadena de ancestros, no solo el padre directo.
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



}
