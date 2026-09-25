package com.uade.tpo.marketplace.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.marketplace.controllers.envios.EnvioResponse;
import com.uade.tpo.marketplace.entity.Envio;
import com.uade.tpo.marketplace.entity.EstadoEnvio;
import com.uade.tpo.marketplace.entity.MetodoEntrega;
import com.uade.tpo.marketplace.entity.OrdenDeCompra;
import com.uade.tpo.marketplace.entity.TipoUsuario;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.CambioDeEstadoNoPermitidoException;
import com.uade.tpo.marketplace.exceptions.EnvioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;
import com.uade.tpo.marketplace.exceptions.TransicionInvalidaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.repository.EnvioRepository;
import com.uade.tpo.marketplace.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EnvioServiceImpl implements EnvioService {
    private final EnvioRepository envioRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Pre : una orden recien pagada.
     * Post: su envio, listo para que el vendedor lo despache. La direccion se
     *       COPIA del perfil del comprador en este momento: es un dato de este
     *       envio puntual, no el domicilio que el usuario tenga cargado el dia
     *       que alguien mire la orden.
     */
    @Transactional
    public void crearParaOrden(OrdenDeCompra orden) {
        if (envioRepository.findByOrdenId(orden.getId()).isPresent())
            return;

        Usuario comprador = orden.getComprador();

        Envio envio = new Envio();
        envio.setOrden(orden);
        envio.setEstado(EstadoEnvio.PENDIENTE);
        envio.setDireccionEntrega(comprador == null || comprador.getDireccion() == null
                ? "Sin direccion cargada"
                : comprador.getDireccion());
        envio.setFechaCreacion(LocalDateTime.now());
        envioRepository.save(envio);
    }

    /**
     * Pre : el id de quien pregunta.
     * Post: sus envios. Un DESPACHANTE no tiene envios propios: recibe la cola
     *       de trabajo compartida, o sea los que el vendedor ya despacho y
     *       todavia no llegaron. Los demas ven aquellos donde compraron o
     *       vendieron. Tira SinResultadosException si no hay ninguno.
     */
    public List<EnvioResponse> getMios(Long idSolicitante)
            throws UsuarioNoEncontradoException, SinResultadosException {
        Usuario usuario = usuarioRepository.findById(idSolicitante)
                .orElseThrow(UsuarioNoEncontradoException::new);

        // La cola del despachante son los despachados y los que ya viajan. Las
        // coordinadas nunca llegan a DESPACHADO, asi que no entran: no hay
        // nada que un despachante tenga que hacer con ellas.
        List<Envio> encontrados;
        if (usuario.getRol() == TipoUsuario.DESPACHANTE)
            encontrados = envioRepository.findByEstadoInOrderByFechaCreacionAsc(
                    List.of(EstadoEnvio.DESPACHADO, EstadoEnvio.EN_TRANSITO));
        else if (usuario.getRol() == TipoUsuario.ADMIN)
            encontrados = envioRepository.findAll();
        else {
            encontrados = new ArrayList<>(
                    envioRepository.findByOrdenCompradorIdOrderByFechaCreacionDesc(idSolicitante));
            encontrados.addAll(
                    envioRepository.findByOrdenVendedorIdOrderByFechaCreacionDesc(idSolicitante));
        }

        if (encontrados.isEmpty())
            throw new SinResultadosException("No hay envios para mostrar");

        return encontrados.stream().map(EnvioResponse::from).toList();
    }

    /**
     * Pre : el id del envio y el de quien pregunta.
     * Post: el envio. Lo ven su comprador, su vendedor y el ADMIN. Un
     *       DESPACHANTE solo los de su cola, ni los pendientes ni los ya
     *       entregados. Tira OperacionAjenaException para cualquier otro.
     */
    public EnvioResponse getById(Long idEnvio, Long idSolicitante)
            throws EnvioNoEncontradoException, OperacionAjenaException,
            UsuarioNoEncontradoException {
        Envio envio = buscar(idEnvio);
        Usuario usuario = usuarioRepository.findById(idSolicitante)
                .orElseThrow(UsuarioNoEncontradoException::new);

        if (!puedeVerlo(envio, usuario))
            throw new OperacionAjenaException();

        return EnvioResponse.from(envio);
    }

    /**
     * Pre : el id del envio, el estado destino y el id de quien lo pide.
     * Post: el envio en el estado nuevo. Al despachar se estampa la fecha y se
     *       genera el numero de seguimiento; al entregar, la fecha de entrega,
     *       que es el hecho que despues habilita calificar al vendedor.
     */
    @Transactional
    public EnvioResponse actualizarEstado(Long idEnvio, EstadoEnvio estado, Long idSolicitante)
            throws EnvioNoEncontradoException, TransicionInvalidaException,
            CambioDeEstadoNoPermitidoException, UsuarioNoEncontradoException {
        Envio envio = buscar(idEnvio);
        Usuario usuario = usuarioRepository.findById(idSolicitante)
                .orElseThrow(UsuarioNoEncontradoException::new);

        validarTransicion(envio, estado);
        validarQuienPuede(envio, estado, usuario);

        if (estado == EstadoEnvio.DESPACHADO) {
            envio.setFechaDespacho(LocalDateTime.now());
            envio.setNumeroSeguimiento("AGRO-%06d".formatted(envio.getId()));
        }

        if (estado == EstadoEnvio.EN_TRANSITO)
            envio.setDespachante(usuario);

        if (estado == EstadoEnvio.ENTREGADO)
            envio.setFechaEntrega(LocalDateTime.now());

        envio.setEstado(estado);
        return EnvioResponse.from(envioRepository.save(envio));
    }

    /**
     * Pre : el estado actual y el destino.
     * Post: nada si el salto existe. El camino es uno solo y no tiene vuelta
     *       atras: cada paso estampa una fecha, y retroceder dejaria fechas
     *       contando algo que no paso.
     */
    private void validarTransicion(Envio envio, EstadoEnvio nuevo)
            throws TransicionInvalidaException {
        boolean coordinado = metodoDe(envio) == MetodoEntrega.COORDINAR;

        boolean permitida = switch (envio.getEstado()) {
            case PENDIENTE -> coordinado
                    ? nuevo == EstadoEnvio.ENTREGADO
                    : nuevo == EstadoEnvio.DESPACHADO;
            case DESPACHADO -> nuevo == EstadoEnvio.EN_TRANSITO;
            case EN_TRANSITO -> nuevo == EstadoEnvio.ENTREGADO;
            case ENTREGADO -> false;
        };

        if (!permitida)
            throw new TransicionInvalidaException();
    }

    private MetodoEntrega metodoDe(Envio envio) {
        return envio.getOrden() == null || envio.getOrden().getMetodoEntrega() == null
                ? MetodoEntrega.DESPACHO
                : envio.getOrden().getMetodoEntrega();
    }

    /**
     * Pre : el envio, el estado destino y quien lo pide.
     * Post: nada si le toca. Despachar es del vendedor, que es el que tiene la
     *       mercaderia. En transito y entregado son del DESPACHANTE: si el
     *       vendedor pudiera declarar la entrega, estaria habilitando el mismo
     *       la resena que lo califica.
     */
    private void validarQuienPuede(Envio envio, EstadoEnvio nuevo, Usuario usuario)
            throws CambioDeEstadoNoPermitidoException {
        if (usuario.getRol() == TipoUsuario.ADMIN)
            return;

        boolean esVendedor = envio.getOrden() != null
                && envio.getOrden().getVendedor() != null
                && envio.getOrden().getVendedor().getId().equals(usuario.getId());

        boolean esComprador = envio.getOrden() != null
                && envio.getOrden().getComprador() != null
                && envio.getOrden().getComprador().getId().equals(usuario.getId());

        boolean esDespachante = usuario.getRol() == TipoUsuario.DESPACHANTE;
        boolean coordinado = metodoDe(envio) == MetodoEntrega.COORDINAR;

        boolean autorizado = switch (nuevo) {
            case DESPACHADO -> esVendedor;
            case EN_TRANSITO -> esDespachante;
            // En las coordinadas no hay despachante que pueda declararlo, asi
            // que lo dice el comprador. Es seguro por la misma razon por la
            // que el vendedor no puede: mentir va en contra de quien lo dice.
            case ENTREGADO -> coordinado ? esComprador : esDespachante;
            case PENDIENTE -> false;
        };

        if (!autorizado)
            throw new CambioDeEstadoNoPermitidoException();
    }

    /**
     * Pre : el envio y quien lo quiere ver.
     * Post: si puede. El DESPACHANTE ve cualquiera porque la cola es
     *       compartida y necesita mirar antes de tomar.
     */
    private boolean puedeVerlo(Envio envio, Usuario usuario) {
        if (usuario.getRol() == TipoUsuario.ADMIN)
            return true;

        // El despachante ve EXACTAMENTE su cola, ni un envio mas. Si pudiera
        // pedir cualquier id recorreria todos y se quedaria con la direccion de
        // entrega de cada compra del sistema, incluidas las coordinadas, que no
        // son asunto suyo. Un envio que ya se entrego tampoco: no queda nada
        // por hacer con el.
        if (usuario.getRol() == TipoUsuario.DESPACHANTE)
            return envio.getEstado() == EstadoEnvio.DESPACHADO
                    || envio.getEstado() == EstadoEnvio.EN_TRANSITO;

        OrdenDeCompra orden = envio.getOrden();
        if (orden == null)
            return false;

        return (orden.getComprador() != null
                && orden.getComprador().getId().equals(usuario.getId()))
                || (orden.getVendedor() != null
                        && orden.getVendedor().getId().equals(usuario.getId()));
    }

    private Envio buscar(Long idEnvio) throws EnvioNoEncontradoException {
        return envioRepository.findById(idEnvio)
                .orElseThrow(EnvioNoEncontradoException::new);
    }
}
