package com.uade.tpo.marketplace.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.marketplace.controllers.envios.EntregaResponse;
import com.uade.tpo.marketplace.controllers.envios.EnvioResponse;
import com.uade.tpo.marketplace.entity.Envio;
import com.uade.tpo.marketplace.entity.EstadoEnvio;
import com.uade.tpo.marketplace.entity.MetodoEntrega;
import com.uade.tpo.marketplace.entity.OrdenDeCompra;
import com.uade.tpo.marketplace.entity.TipoUsuario;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.CambioDeEstadoNoPermitidoException;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.EnvioNoDisponibleException;
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
    private static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom AZAR = new SecureRandom();

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
     * Post: sus envios. Un DESPACHANTE ve los que tiene en la mano: los que el
     *       mismo cargo por numero y todavia no entrego. Los demas ven aquellos
     *       donde compraron o vendieron. Tira SinResultadosException si no hay
     *       ninguno.
     */
    public List<EnvioResponse> getMios(Long idSolicitante)
            throws UsuarioNoEncontradoException, SinResultadosException {
        Usuario usuario = usuarioRepository.findById(idSolicitante)
                .orElseThrow(UsuarioNoEncontradoException::new);

        // El despachante ve los que TIENE, no los que existen: los que cargo
        // por numero y todavia no entrego. No hay una cola de pendientes para
        // mirar, porque mirarla seria ver las direcciones de entrega de
        // paquetes que no tiene.
        List<Envio> encontrados;
        if (usuario.getRol() == TipoUsuario.DESPACHANTE)
            encontrados = envioRepository.findByDespachanteIdAndEstadoOrderByFechaDespachoAsc(
                    idSolicitante, EstadoEnvio.EN_TRANSITO);
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
     * Pre : el id de quien pregunta, que tiene que ser DESPACHANTE.
     * Post: lo que entrego, lo mas nuevo primero, con el numero y la fecha y
     *       nada mas. El historial es de EL: cuenta lo que hizo, no por quienes
     *       paso. Tira SinResultadosException si todavia no entrego nada.
     */
    public List<EntregaResponse> getHistorial(Long idSolicitante)
            throws UsuarioNoEncontradoException, AccesoDenegadoException,
            SinResultadosException {
        Usuario usuario = usuarioRepository.findById(idSolicitante)
                .orElseThrow(UsuarioNoEncontradoException::new);

        if (usuario.getRol() != TipoUsuario.DESPACHANTE)
            throw new AccesoDenegadoException(
                    "Solo un despachante tiene historial de entregas");

        List<Envio> entregados = envioRepository
                .findByDespachanteIdAndEstadoOrderByFechaEntregaDesc(
                        idSolicitante, EstadoEnvio.ENTREGADO);

        if (entregados.isEmpty())
            throw new SinResultadosException("Todavia no entregaste ningun envio");

        return entregados.stream().map(EntregaResponse::from).toList();
    }

    /**
     * Pre : el numero que figura en la etiqueta del bulto y el id de quien lo
     *       carga, que tiene que ser DESPACHANTE.
     * Post: el envio, ya asignado a el y EN_TRANSITO. Cargar el numero ES
     *       tomar el envio: no hay una cola para elegir, porque en una sucursal
     *       uno carga el paquete que tiene en la mano, no uno de una lista. Por
     *       eso tambien es el momento en que aparece la direccion de entrega.
     *       404 si ese numero no existe, 409 si ya lo tomo otro o si el vendedor
     *       todavia no lo despacho.
     */
    @Transactional
    public EnvioResponse recibir(String numeroSeguimiento, Long idSolicitante)
            throws EnvioNoEncontradoException, EnvioNoDisponibleException,
            CambioDeEstadoNoPermitidoException, UsuarioNoEncontradoException {
        Usuario usuario = usuarioRepository.findById(idSolicitante)
                .orElseThrow(UsuarioNoEncontradoException::new);

        if (usuario.getRol() != TipoUsuario.DESPACHANTE)
            throw new CambioDeEstadoNoPermitidoException();

        Envio envio = envioRepository
                .findByNumeroSeguimiento(numeroSeguimiento == null
                        ? "" : numeroSeguimiento.trim().toUpperCase())
                .orElseThrow(EnvioNoEncontradoException::new);

        if (envio.getEstado() != EstadoEnvio.DESPACHADO)
            throw new EnvioNoDisponibleException();

        envio.setDespachante(usuario);
        envio.setEstado(EstadoEnvio.EN_TRANSITO);
        return EnvioResponse.from(envioRepository.save(envio));
    }

    /**
     * Pre : nada.
     * Post: un numero que no existia. Sin las letras y numeros que se confunden
     *       leyendo una etiqueta, porque alguien lo va a tipear a mano. Al azar
     *       y no correlativo: es lo que destraba una direccion de entrega, y uno
     *       correlativo se adivina probando desde el uno.
     */
    private String nuevoNumero() {
        String candidato;
        do {
            StringBuilder sb = new StringBuilder("AGRO-");
            for (int i = 0; i < 10; i++)
                sb.append(ALFABETO.charAt(AZAR.nextInt(ALFABETO.length())));
            candidato = sb.toString();
        } while (envioRepository.existsByNumeroSeguimiento(candidato));

        return candidato;
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
            envio.setNumeroSeguimiento(nuevoNumero());
        }

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

        // No cualquier despachante: el que lo cargo. Si no, uno entrega lo que
        // otro esta llevando.
        boolean esSuDespachante = usuario.getRol() == TipoUsuario.DESPACHANTE
                && envio.getDespachante() != null
                && envio.getDespachante().getId().equals(usuario.getId());
        boolean coordinado = metodoDe(envio) == MetodoEntrega.COORDINAR;

        boolean autorizado = switch (nuevo) {
            case DESPACHADO -> esVendedor;
            // El paso a EN_TRANSITO no se pide por aca: se logra cargando el
            // numero de seguimiento, que es lo que prueba tener el paquete.
            case EN_TRANSITO -> false;
            // En las coordinadas no hay despachante que pueda declararlo, asi
            // que lo dice el comprador. Es seguro por la misma razon por la
            // que el vendedor no puede: mentir va en contra de quien lo dice.
            case ENTREGADO -> coordinado ? esComprador : esSuDespachante;
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

        // El despachante ve lo que tiene en la mano y nada mas: el que el mismo
        // cargo y todavia no entrego. Por id podria recorrer todos y quedarse
        // con la direccion de entrega de cada compra del sistema.
        if (usuario.getRol() == TipoUsuario.DESPACHANTE)
            return envio.getEstado() == EstadoEnvio.EN_TRANSITO
                    && envio.getDespachante() != null
                    && envio.getDespachante().getId().equals(usuario.getId());

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
