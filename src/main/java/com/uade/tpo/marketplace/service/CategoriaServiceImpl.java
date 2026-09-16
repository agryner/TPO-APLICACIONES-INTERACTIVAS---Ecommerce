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
import com.uade.tpo.marketplace.exceptions.CategoriaPadreInactivaException;
import com.uade.tpo.marketplace.exceptions.JerarquiaInvalidaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;
import com.uade.tpo.marketplace.repository.CategoriaRepository;
import com.uade.tpo.marketplace.repository.ProductoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final AutorizacionService autorizacion;

    public List<CategoriaResponse> getCategorias() throws SinResultadosException {
        List<CategoriaResponse> todas = categoriaRepository.findByActivoTrue().stream()
                .map(CategoriaResponse::from)
                .toList();

        if (todas.isEmpty())
            throw new SinResultadosException("No hay categorias cargadas");

        return todas;
    }

    public List<CategoriaResponse> getCategoriasRaiz() throws SinResultadosException {
        List<CategoriaResponse> raices = categoriaRepository.findByCategoriaPadreIsNullAndActivoTrue()
                .stream()
                .map(CategoriaResponse::from)
                .toList();

        if (raices.isEmpty())
            throw new SinResultadosException("No hay categorias de primer nivel");

        return raices;
    }

    /**
     * Pre : el id del padre.
     * Post: sus hijas inmediatas, sin nietas. Tira
     *       CategoriaNoEncontradaException si el padre no existe.
     */
    public List<CategoriaResponse> getSubcategorias(Long idCategoria)
            throws CategoriaNoEncontradaException, SinResultadosException {
        buscarActiva(idCategoria);

        List<CategoriaResponse> hijas = categoriaRepository
                .findByCategoriaPadreIdAndActivoTrue(idCategoria).stream()
                .map(CategoriaResponse::from)
                .toList();

        if (hijas.isEmpty())
            throw new SinResultadosException("Esa categoria no tiene subcategorias");

        return hijas;
    }

    public CategoriaResponse getCategoriaById(Long idCategoria) throws CategoriaNoEncontradaException {
        return CategoriaResponse.from(buscarActiva(idCategoria));
    }

    /**
     * Pre : el id de una categoria.
     * Post: la entidad. Tira CategoriaNoEncontradaException si no existe o si
     *       esta dada de baja: para todo el resto del sistema una categoria
     *       inactiva es una categoria que no esta.
     */
    private Categoria buscarActiva(Long idCategoria) throws CategoriaNoEncontradaException {
        Categoria categoria = categoriaRepository.findById(idCategoria)
                .orElseThrow(CategoriaNoEncontradaException::new);

        if (!Boolean.TRUE.equals(categoria.getActivo()))
            throw new CategoriaNoEncontradaException();

        return categoria;
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

        Categoria categoria = buscarActiva(idCategoria);

        Categoria padre = buscarPadre(request.getIdCategoriaPadre());
        validarJerarquia(categoria, padre);
        validarNombreLibre(request.getNombre(), padre, idCategoria);

        categoria.setNombre(request.getNombre());
        categoria.setDescripcion(request.getDescripcion());
        categoria.setCategoriaPadre(padre);
        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    /**
     * Pre : el id de la categoria y el de quien pide, que tiene que ser ADMIN.
     * Post: la categoria de vuelta en circulacion. No reactiva sus
     *       subcategorias: cada una se reactiva por separado, igual que las
     *       publicaciones de un usuario. Tira CategoriaPadreInactivaException
     *       si su padre sigue de baja, porque quedaria colgando de algo que no
     *       existe, y CategoriaDuplicadaException si mientras estuvo de baja
     *       alguien le ocupo el nombre entre sus hermanas.
     */
    public CategoriaResponse reactivarCategoria(Long idCategoria, Long idSolicitante)
            throws CategoriaNoEncontradaException, CategoriaDuplicadaException,
            CategoriaPadreInactivaException, UsuarioNoEncontradoException,
            AccesoDenegadoException {
        autorizacion.validarAdmin(idSolicitante);

        Categoria categoria = categoriaRepository.findById(idCategoria)
                .orElseThrow(CategoriaNoEncontradaException::new);

        Categoria padre = categoria.getCategoriaPadre();
        if (padre != null && !Boolean.TRUE.equals(padre.getActivo()))
            throw new CategoriaPadreInactivaException();

        validarNombreLibre(categoria.getNombre(), padre, idCategoria);

        categoria.setActivo(true);
        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    /**
     * Pre : el id y el id de quien pide.
     * Post: nada. Es baja logica: la fila queda y deja de aparecer en todos
     *       los listados. Tira CategoriaConSubcategoriasException o
     *       CategoriaConProductosException si no esta vacia.
     */
    public void deleteCategoria(Long idCategoria, Long idSolicitante)
            throws CategoriaNoEncontradaException, CategoriaConSubcategoriasException,
            CategoriaConProductosException, UsuarioNoEncontradoException,
            AccesoDenegadoException {
        autorizacion.validarAdmin(idSolicitante);

        Categoria categoria = buscarActiva(idCategoria);

        if (!categoriaRepository.findByCategoriaPadreIdAndActivoTrue(idCategoria).isEmpty())
            throw new CategoriaConSubcategoriasException();

        if (productoRepository.existsByCategoriaId(idCategoria))
            throw new CategoriaConProductosException();

        categoria.setActivo(false);
        categoriaRepository.save(categoria);
    }

    /**
     * Pre : el id del padre, que puede venir en null.
     * Post: la entidad, o null si no se indico ninguno. Tira
     *       CategoriaNoEncontradaException si el id no existe.
     */
    private Categoria buscarPadre(Long idCategoriaPadre) throws CategoriaNoEncontradaException {
        if (idCategoriaPadre == null)
            return null;

        return buscarActiva(idCategoriaPadre);
    }

    /**
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
                ? categoriaRepository.findByCategoriaPadreIsNullAndActivoTrue()
                : categoriaRepository.findByCategoriaPadreIdAndActivoTrue(padre.getId());
    }

    /**
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
