package com.uade.tpo.marketplace.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.marketplace.controllers.resenas.CalificacionResponse;
import com.uade.tpo.marketplace.controllers.resenas.ResenaRequest;
import com.uade.tpo.marketplace.controllers.resenas.ResenaResponse;
import com.uade.tpo.marketplace.entity.Envio;
import com.uade.tpo.marketplace.entity.EstadoEnvio;
import com.uade.tpo.marketplace.entity.OrdenDeCompra;
import com.uade.tpo.marketplace.entity.OrderDetail;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.entity.Resena;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.EntregaPendienteException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.OrdenNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.ProductoFueraDeLaOrdenException;
import com.uade.tpo.marketplace.exceptions.ResenaDuplicadaException;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.repository.EnvioRepository;
import com.uade.tpo.marketplace.repository.OrdenDeCompraRepository;
import com.uade.tpo.marketplace.repository.ProductoRepository;
import com.uade.tpo.marketplace.repository.ResenaRepository;
import com.uade.tpo.marketplace.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResenaService {
    private final ResenaRepository resenaRepository;
    private final OrdenDeCompraRepository ordenRepository;
    private final EnvioRepository envioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;

    /**
     * Pre : el request con la orden, el producto y el puntaje, mas el id de
     *       quien califica.
     * Post: la resena guardada. Solo la deja el comprador de esa orden, solo
     *       sobre un producto que esa orden contiene, y solo despues de que el
     *       envio figure ENTREGADO. Esa ultima condicion es la que le da valor:
     *       la entrega la declara el despachante, o el propio comprador si la
     *       entrega fue coordinada, nunca el vendedor que va a ser calificado.
     */
    @Transactional
    public ResenaResponse crear(ResenaRequest request, Long idSolicitante)
            throws OrdenNoEncontradaException, OperacionAjenaException,
            ProductoFueraDeLaOrdenException, EntregaPendienteException,
            ResenaDuplicadaException, UsuarioNoEncontradoException {
        OrdenDeCompra orden = ordenRepository.findById(request.getIdOrden())
                .orElseThrow(OrdenNoEncontradaException::new);

        if (orden.getComprador() == null
                || !orden.getComprador().getId().equals(idSolicitante))
            throw new OperacionAjenaException();

        Producto producto = productoDeLaOrden(orden, request.getIdProducto());

        Envio envio = envioRepository.findByOrdenId(orden.getId())
                .orElseThrow(EntregaPendienteException::new);

        if (envio.getEstado() != EstadoEnvio.ENTREGADO)
            throw new EntregaPendienteException();

        if (resenaRepository.existsByOrdenIdAndProductoId(orden.getId(), producto.getId()))
            throw new ResenaDuplicadaException();

        Usuario autor = usuarioRepository.findById(idSolicitante)
                .orElseThrow(UsuarioNoEncontradoException::new);

        Resena resena = new Resena();
        resena.setOrden(orden);
        resena.setProducto(producto);
        resena.setAutor(autor);
        resena.setPuntaje(request.getPuntaje());
        resena.setComentario(request.getComentario());
        resena.setFecha(LocalDateTime.now());

        return ResenaResponse.from(resenaRepository.save(resena));
    }

    /**
     * Pre : la orden y el id de un producto.
     * Post: el producto, si esa orden lo contiene. Se busca entre los
     *       renglones y no en el catalogo: calificar algo que no compraste en
     *       esta orden no tiene sentido.
     */
    private Producto productoDeLaOrden(OrdenDeCompra orden, Long idProducto)
            throws ProductoFueraDeLaOrdenException {
        for (OrderDetail renglon : orden.getItems()) {
            Producto producto = renglon.getProducto();
            if (producto != null && producto.getId().equals(idProducto))
                return producto;
        }

        throw new ProductoFueraDeLaOrdenException();
    }

    /**
     * Pre : el id de un producto.
     * Post: sus resenas, las mas nuevas primero. Es publico: quien esta
     *       decidiendo si comprar necesita leerlas sin tener cuenta.
     */
    public List<ResenaResponse> getDeProducto(Long idProducto) throws SinResultadosException {
        List<ResenaResponse> resenas = resenaRepository
                .findByProductoIdOrderByFechaDesc(idProducto).stream()
                .map(ResenaResponse::from)
                .toList();

        if (resenas.isEmpty())
            throw new SinResultadosException("Ese producto todavia no tiene resenas");

        return resenas;
    }

    /**
     * Pre : el nombre de usuario de un vendedor.
     * Post: el promedio de todas las resenas de sus productos y cuantas son.
     *       No se guarda en ningun lado: se calcula al preguntar, asi no puede
     *       quedar desactualizado. Un vendedor sin resenas devuelve promedio
     *       null, que no es lo mismo que cero.
     */
    public CalificacionResponse getCalificacion(String nombreUsuario)
            throws UsuarioNoEncontradoException {
        Usuario vendedor = usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);

        List<Resena> resenas = resenaRepository.findByProductoVendedorId(vendedor.getId());

        if (resenas.isEmpty())
            return new CalificacionResponse(vendedor.getNombreUsuario(), null, 0);

        double promedio = resenas.stream().mapToInt(Resena::getPuntaje).average().orElse(0);

        return new CalificacionResponse(vendedor.getNombreUsuario(),
                Math.round(promedio * 100) / 100.0, resenas.size());
    }
}
