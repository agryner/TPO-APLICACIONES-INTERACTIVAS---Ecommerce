package com.uade.tpo.marketplace.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.marketplace.controllers.destacados.DestacadoResponse;
import com.uade.tpo.marketplace.controllers.productos.ProductoResponse;
import com.uade.tpo.marketplace.entity.Destacado;
import com.uade.tpo.marketplace.entity.NivelDestacado;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.entity.TipoUsuario;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.ProductoNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.repository.DestacadoRepository;
import com.uade.tpo.marketplace.repository.ProductoRepository;
import com.uade.tpo.marketplace.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DestacadoServiceImpl implements DestacadoService {
    private final DestacadoRepository destacadoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AutorizacionService autorizacion;

    /**
     * Pre : el producto, el nivel, cuantos meses y el id de quien lo otorga,
     *       que tiene que ser ADMIN.
     * Post: el producto con su visibilidad al dia, y una fila de historial. Lo
     *       asigna el ADMIN porque no hay pasarela: la plata se cobra por
     *       afuera y aca se registra que se vendio. Con NINGUNO se saca, y esa
     *       baja no deja historial porque no se vendio nada.
     */
    @Transactional
    public ProductoResponse destacar(Long idProducto, NivelDestacado nivel, Integer meses,
            Long idSolicitante)
            throws ProductoNoEncontradoException, UsuarioNoEncontradoException,
            AccesoDenegadoException {
        autorizacion.validarAdmin(idSolicitante);

        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(ProductoNoEncontradoException::new);

        if (nivel == NivelDestacado.NINGUNO) {
            producto.setNivelDestacado(NivelDestacado.NINGUNO);
            producto.setDestacadoHasta(null);
            return ProductoResponse.from(productoRepository.save(producto));
        }

        int cuantos = meses == null || meses < 1 ? 1 : meses;
        LocalDateTime desde = LocalDateTime.now();
        LocalDateTime hasta = desde.plusMonths(cuantos);

        producto.setNivelDestacado(nivel);
        producto.setDestacadoHasta(hasta);
        productoRepository.save(producto);

        Usuario admin = usuarioRepository.findById(idSolicitante)
                .orElseThrow(UsuarioNoEncontradoException::new);

        Destacado registro = new Destacado();
        registro.setProducto(producto);
        registro.setNivel(nivel);
        registro.setMeses(cuantos);
        registro.setDesde(desde);
        registro.setHasta(hasta);
        registro.setOtorgadoPor(admin);
        destacadoRepository.save(registro);

        return ProductoResponse.from(producto);
    }

    /**
     * Pre : el id de quien pregunta.
     * Post: el historial de visibilidad. El ADMIN ve el de todos, para saber
     *       que se vendio; un vendedor ve solo el de sus productos. Cada fila
     *       dice que nivel, cuantos meses y desde cuando: con eso se puede
     *       contar cuanto PREMIUM compro alguien.
     */
    public List<DestacadoResponse> getHistorial(Long idSolicitante)
            throws UsuarioNoEncontradoException, SinResultadosException {
        Usuario usuario = usuarioRepository.findById(idSolicitante)
                .orElseThrow(UsuarioNoEncontradoException::new);

        List<Destacado> filas = usuario.getRol() == TipoUsuario.ADMIN
                ? destacadoRepository.findByOrderByDesdeDesc()
                : destacadoRepository.findByProductoVendedorIdOrderByDesdeDesc(idSolicitante);

        if (filas.isEmpty())
            throw new SinResultadosException("No hay destacados para mostrar");

        return filas.stream().map(DestacadoResponse::from).toList();
    }

    /**
     * Pre : nada. La llama la tarea programada.
     * Post: cuantos bajo. El orden del catalogo ya ignora los vencidos mirando
     *       la fecha, asi que esto no cambia lo que ve nadie: sirve para que
     *       la columna no siga diciendo PREMIUM sobre algo que se termino.
     */
    @Transactional
    public int bajarLosVencidos() {
        List<Producto> vencidos = productoRepository.findAll().stream()
                .filter(p -> p.getNivelDestacado() != null
                        && p.getNivelDestacado() != NivelDestacado.NINGUNO)
                .filter(p -> p.getDestacadoHasta() == null
                        || p.getDestacadoHasta().isBefore(LocalDateTime.now()))
                .toList();

        for (Producto producto : vencidos) {
            producto.setNivelDestacado(NivelDestacado.NINGUNO);
            producto.setDestacadoHasta(null);
            productoRepository.save(producto);
        }

        return vencidos.size();
    }
}
