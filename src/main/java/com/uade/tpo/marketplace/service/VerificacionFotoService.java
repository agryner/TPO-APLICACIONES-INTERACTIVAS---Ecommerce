package com.uade.tpo.marketplace.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.uade.tpo.marketplace.entity.EstadoPublicacion;
import com.uade.tpo.marketplace.entity.EstadoVerificacion;
import com.uade.tpo.marketplace.entity.Foto;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.entity.TipoNotificacion;
import com.uade.tpo.marketplace.entity.TipoUsuario;
import com.uade.tpo.marketplace.repository.FotoRepository;
import com.uade.tpo.marketplace.repository.ProductoRepository;
import com.uade.tpo.marketplace.repository.UsuarioRepository;

/**
 * Verificacion de fotos con IA en segundo plano. Gemini puede tardar 40
 * segundos o mas por imagen: si la subida lo esperara, el vendedor se quedaria
 * mirando un "cargando". La foto se guarda EN_REVISION, la subida responde al
 * toque, y cuando la IA termina se le avisa al vendedor con una notificacion.
 */
@Service
public class VerificacionFotoService {
    private static final Logger log = LoggerFactory.getLogger(VerificacionFotoService.class);

    @Value("${marketplace.ia.umbral-aprobacion:0.7}")
    private double umbralAprobacion;

    @Value("${marketplace.ia.umbral-rechazo:0.4}")
    private double umbralRechazo;

    private final FotoRepository fotoRepository;
    private final ProductoRepository productoRepository;
    private final VerificadorImagenService verificador;
    private final NotificacionService notificacionService;
    private final UsuarioRepository usuarioRepository;
    // Para que las llamadas @Async pasen por el proxy de Spring, el servicio se
    // llama a si mismo a traves de esta referencia y no con "this".
    private final VerificacionFotoService self;

    public VerificacionFotoService(FotoRepository fotoRepository, ProductoRepository productoRepository,
            VerificadorImagenService verificador, NotificacionService notificacionService,
            UsuarioRepository usuarioRepository, @Lazy VerificacionFotoService self) {
        this.fotoRepository = fotoRepository;
        this.productoRepository = productoRepository;
        this.verificador = verificador;
        this.notificacionService = notificacionService;
        this.usuarioRepository = usuarioRepository;
        this.self = self;
    }

    /**
     * Pre : el id de una foto recien guardada, dentro de la transaccion que la
     *       guarda.
     * Post: nada todavia: la verificacion arranca cuando esa transaccion se
     *       confirma. Antes, el hilo de fondo no veria la foto en la base.
     */
    public void programar(Long idFoto) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    self.verificar(idFoto);
                }
            });
        } else {
            self.verificar(idFoto);
        }
    }

    /**
     * Pre : el id de un producto cuya categoria acaba de cambiar.
     * Post: sus fotos EN_REVISION y RECHAZADA vuelven a pasar por la IA con la
     *       categoria nueva. Una foto rechazada por "no es de esta categoria"
     *       puede ser correcta en la nueva.
     */
    public void reverificarProducto(Long idProducto) {
        List<Long> ids = fotoRepository.findByProductoIdAndActivoTrue(idProducto).stream()
                .filter(f -> f.getEstadoVerificacion() != EstadoVerificacion.APROBADA)
                .map(Foto::getId)
                .toList();
        ids.forEach(this::programar);
    }

    /**
     * Pre : el id de una foto ya guardada.
     * Post: la foto con el veredicto de la IA (APROBADA, RECHAZADA, o
     *       EN_REVISION si dudo o no respondio), el producto publicado si con
     *       esta foto corresponde, y el vendedor avisado del resultado.
     */
    @Async
    @Transactional
    public void verificar(Long idFoto) {
        Foto foto = fotoRepository.findById(idFoto).orElse(null);
        if (foto == null || !Boolean.TRUE.equals(foto.getActivo()))
            return;

        Producto producto = foto.getProducto();
        VerificadorImagenService.Resultado resultado = consultarConReintentos(foto, producto);
        if (resultado == null) {
            foto.setEstadoVerificacion(EstadoVerificacion.EN_REVISION);
            fotoRepository.save(foto);
            avisar(producto, "La revision automatica no esta disponible ahora: un administrador va a revisar tu foto de \""
                    + producto.getNombre() + "\".");
            avisarAdmins(producto);
            return;
        }

        double puntaje = resultado.puntaje();
        foto.setConfianzaIa(puntaje);
        foto.setQueVeIa(resultado.queVeo());

        if (puntaje < umbralRechazo) {
            foto.setEstadoVerificacion(EstadoVerificacion.RECHAZADA);
            fotoRepository.save(foto);
            String motivo = resultado.mensajeAlVendedor() != null
                    ? resultado.mensajeAlVendedor()
                    : "no se corresponde con la categoria del producto";
            avisar(producto, "Rechazamos una foto de \"" + producto.getNombre() + "\": " + motivo
                    + " Si la categoria estaba mal, corregila y la volvemos a revisar.");
            return;
        }

        if (puntaje > umbralAprobacion) {
            foto.setEstadoVerificacion(EstadoVerificacion.APROBADA);
            fotoRepository.save(foto);
            boolean publicado = publicarSiCorresponde(foto);
            avisar(producto, publicado
                    ? "Aprobamos la foto de \"" + producto.getNombre() + "\". Tu publicacion ya esta visible."
                    : "Aprobamos una foto nueva de \"" + producto.getNombre() + "\".");
            return;
        }

        foto.setEstadoVerificacion(EstadoVerificacion.EN_REVISION);
        fotoRepository.save(foto);
        avisar(producto, "Una foto de \"" + producto.getNombre()
                + "\" quedo para revision manual: un administrador la va a mirar.");
        avisarAdmins(producto);
    }

    private static final int INTENTOS = 3;
    private static final long ESPERA_ENTRE_INTENTOS_MS = 20_000;

    /**
     * Pre : la foto y su producto.
     * Post: el veredicto de la IA, o null si no respondio despues de
     *       INTENTOS. Gemini contesta seguido 503 por exceso de demanda y se
     *       recupera en segundos: como esto corre en segundo plano, esperar y
     *       reintentar no le cuesta nada a nadie.
     */
    private VerificadorImagenService.Resultado consultarConReintentos(Foto foto, Producto producto) {
        for (int intento = 1; intento <= INTENTOS; intento++) {
            try {
                return verificador.verificar(foto.getContenido(), foto.getTipoContenido(),
                        producto.getCategoria());
            } catch (Exception e) {
                log.warn("Intento {}/{}: no se pudo verificar la foto {} del producto {}: {}",
                        intento, INTENTOS, foto.getId(), producto.getId(), e.getMessage());
                if (intento == INTENTOS)
                    return null;
                try {
                    Thread.sleep(ESPERA_ENTRE_INTENTOS_MS);
                } catch (InterruptedException interrumpido) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Pre : una foto ya guardada.
     * Post: si esta APROBADA y su producto estaba en BORRADOR, el producto
     *       queda PUBLICADO y devuelve true. Un producto solo se publica con una
     *       foto que el comprador pueda ver. PAUSADO no se toca: lo pauso el
     *       vendedor a proposito.
     */
    public boolean publicarSiCorresponde(Foto foto) {
        Producto producto = foto.getProducto();
        if (foto.getEstadoVerificacion() == EstadoVerificacion.APROBADA
                && producto.getEstadoPublicacion() == EstadoPublicacion.BORRADOR) {
            producto.setEstadoPublicacion(EstadoPublicacion.PUBLICADO);
            productoRepository.save(producto);
            return true;
        }
        return false;
    }

    /**
     * Pre : el producto de una foto que quedo EN_REVISION.
     * Post: cada ADMIN activo con un aviso que lo lleva a las fotos
     *       pendientes. Sin esto la foto espera hasta que alguno entre de
     *       casualidad a la pestana.
     */
    private void avisarAdmins(Producto producto) {
        String texto = "Hay una foto de \"" + producto.getNombre() + "\" esperando revision manual.";
        String recortado = texto.length() <= 300 ? texto : texto.substring(0, 297) + "...";
        usuarioRepository.findByRolAndActivoTrue(TipoUsuario.ADMIN).forEach(admin ->
                notificacionService.crear(admin, TipoNotificacion.FOTO_PENDIENTE, recortado, "/fotos/pendientes"));
    }

    private void avisar(Producto producto, String mensaje) {
        String texto = mensaje.length() <= 300 ? mensaje : mensaje.substring(0, 297) + "...";
        notificacionService.crear(producto.getVendedor(), TipoNotificacion.FOTO_VERIFICADA, texto,
                "/mis-publicaciones");
    }
}
