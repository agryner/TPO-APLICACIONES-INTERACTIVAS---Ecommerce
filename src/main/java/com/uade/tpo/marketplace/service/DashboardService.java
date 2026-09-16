package com.uade.tpo.marketplace.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.uade.tpo.marketplace.controllers.dashboard.DashboardResponse;
import com.uade.tpo.marketplace.controllers.dashboard.DashboardResponse.Plata;
import com.uade.tpo.marketplace.controllers.dashboard.DashboardResponse.ProductoDestacado;
import com.uade.tpo.marketplace.controllers.dashboard.DashboardResponse.Rendimiento;
import com.uade.tpo.marketplace.controllers.dashboard.DashboardResponse.RequiereAccion;
import com.uade.tpo.marketplace.controllers.dashboard.DashboardResponse.Reputacion;
import com.uade.tpo.marketplace.controllers.resenas.ResenaResponse;
import com.uade.tpo.marketplace.entity.Envio;
import com.uade.tpo.marketplace.entity.EstadoEnvio;
import com.uade.tpo.marketplace.entity.EstadoOrden;
import com.uade.tpo.marketplace.entity.EstadoPublicacion;
import com.uade.tpo.marketplace.entity.EstadoVerificacion;
import com.uade.tpo.marketplace.entity.Foto;
import com.uade.tpo.marketplace.entity.MetodoEntrega;
import com.uade.tpo.marketplace.entity.OrdenDeCompra;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.entity.Resena;
import com.uade.tpo.marketplace.exceptions.RolNoComerciaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.repository.EnvioRepository;
import com.uade.tpo.marketplace.repository.FotoRepository;
import com.uade.tpo.marketplace.repository.ItemWishlistRepository;
import com.uade.tpo.marketplace.repository.OrdenDeCompraRepository;
import com.uade.tpo.marketplace.repository.ProductoRepository;
import com.uade.tpo.marketplace.repository.ResenaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final ProductoRepository productoRepository;
    private final OrdenDeCompraRepository ordenRepository;
    private final EnvioRepository envioRepository;
    private final FotoRepository fotoRepository;
    private final ResenaRepository resenaRepository;
    private final ItemWishlistRepository itemWishlistRepository;
    private final AutorizacionService autorizacion;

    /**
     * Pre : el id del vendedor.
     * Post: las cuatro secciones del tablero. Las cuentas se hacen del lado del
     *       servidor a proposito: si las hiciera el front, dos pantallas
     *       podrian mostrar numeros distintos del mismo hecho. Tira
     *       RolNoComerciaException para el ADMIN y el DESPACHANTE, que no
     *       venden y por lo tanto no tienen tablero.
     */
    public DashboardResponse getDelVendedor(Long idVendedor)
            throws UsuarioNoEncontradoException, RolNoComerciaException {
        autorizacion.validarQuePuedaComerciar(idVendedor);

        List<Producto> productos = productoRepository.findByVendedorIdAndActivoTrue(idVendedor);
        List<OrdenDeCompra> ordenes = ordenRepository.findByVendedorId(idVendedor);
        List<Envio> envios = envioRepository.findByOrdenVendedorIdOrderByFechaCreacionDesc(idVendedor);
        List<Resena> resenas = resenaRepository.findByProductoVendedorId(idVendedor);

        return new DashboardResponse(
                requiereAccion(productos, envios),
                plata(ordenes),
                rendimiento(productos),
                reputacion(resenas, envios));
    }

    /**
     * Pre : los productos y envios del vendedor.
     * Post: lo que esta esperando que haga algo. Va primero en la respuesta
     *       porque es lo unico del tablero sobre lo que puede actuar hoy: un
     *       borrador sin foto es una publicacion que nadie ve y que el vendedor
     *       suele no saber por que no vende.
     */
    private RequiereAccion requiereAccion(List<Producto> productos, List<Envio> envios) {
        long borradores = productos.stream()
                .filter(p -> p.getEstadoPublicacion() == EstadoPublicacion.BORRADOR)
                .count();

        long enRevision = productos.stream()
                .flatMap(p -> fotoRepository.findByProductoIdAndActivoTrue(p.getId()).stream())
                .filter(f -> f.getEstadoVerificacion() == EstadoVerificacion.EN_REVISION)
                .map(Foto::getId)
                .count();

        long aDespachar = envios.stream()
                .filter(e -> e.getEstado() == EstadoEnvio.PENDIENTE)
                .filter(e -> metodoDe(e) == MetodoEntrega.DESPACHO)
                .count();

        long aCoordinar = envios.stream()
                .filter(e -> e.getEstado() == EstadoEnvio.PENDIENTE)
                .filter(e -> metodoDe(e) == MetodoEntrega.COORDINAR)
                .count();

        return new RequiereAccion(borradores, aDespachar, aCoordinar, enRevision);
    }

    /**
     * Pre : las ordenes donde el usuario vendio.
     * Post: cuanto entro y cuanto esta esperando. Solo cuentan las PAGADA: una
     *       orden PENDIENTE es una venta que todavia no se cobro, y sumarla a
     *       los ingresos seria contar plata que no llego.
     */
    private Plata plata(List<OrdenDeCompra> ordenes) {
        LocalDateTime inicioDeMes = LocalDateTime.now()
                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        List<OrdenDeCompra> pagadas = ordenes.stream()
                .filter(o -> o.getEstado() == EstadoOrden.PAGADA)
                .toList();

        BigDecimal ingresos = pagadas.stream()
                .map(OrdenDeCompra::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal esteMes = pagadas.stream()
                .filter(o -> o.getFechaCreacion() != null
                        && o.getFechaCreacion().isAfter(inicioDeMes))
                .map(OrdenDeCompra::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendientes = ordenes.stream()
                .filter(o -> o.getEstado() == EstadoOrden.PENDIENTE).count();

        long canceladas = ordenes.stream()
                .filter(o -> o.getEstado() == EstadoOrden.CANCELADA).count();

        return new Plata(ingresos, esteMes, pagadas.size(), pendientes, canceladas);
    }

    /**
     * Pre : los productos del vendedor.
     * Post: que tan bien andan sus publicaciones. La conversion cruza visitas
     *       con ventas: un producto muy visto que no vende suele ser un
     *       problema de precio, y uno que nadie mira, de titulo o de foto. Los
     *       guardados en wishlist son demanda contenida: gente que lo quiere y
     *       todavia no lo compro.
     */
    private Rendimiento rendimiento(List<Producto> productos) {
        long visitas = productos.stream()
                .mapToLong(p -> p.getVistos() == null ? 0 : p.getVistos()).sum();

        long vendidos = productos.stream()
                .mapToLong(p -> p.getVendidos() == null ? 0 : p.getVendidos()).sum();

        long guardados = productos.stream()
                .mapToLong(p -> itemWishlistRepository.findByProductoId(p.getId()).size()).sum();

        Double conversion = visitas == 0 ? null
                : Math.round(vendidos * 10000.0 / visitas) / 100.0;

        return new Rendimiento(visitas, conversion, guardados,
                destacado(productos, p -> p.getVendidos() == null ? 0 : p.getVendidos()),
                destacado(productos, p -> p.getVistos() == null ? 0 : p.getVistos()));
    }

    /**
     * Pre : las resenas de sus productos y sus envios.
     * Post: como lo ven los compradores y cuanto tarda en despachar. El tiempo
     *       sale de las dos fechas del envio, y es el dato que despues puede
     *       alimentar un ranking: cuanto tarda no lo declara el vendedor, sale
     *       de cuando efectivamente movio la mercaderia.
     */
    private Reputacion reputacion(List<Resena> resenas, List<Envio> envios) {
        Double promedio = resenas.isEmpty() ? null
                : Math.round(resenas.stream().mapToInt(Resena::getPuntaje).average().orElse(0)
                        * 100) / 100.0;

        List<Long> horas = envios.stream()
                .filter(e -> e.getFechaDespacho() != null && e.getFechaCreacion() != null)
                .map(e -> Duration.between(e.getFechaCreacion(), e.getFechaDespacho()).toHours())
                .toList();

        Double dias = horas.isEmpty() ? null
                : Math.round(horas.stream().mapToLong(Long::longValue).average().orElse(0)
                        / 24.0 * 100) / 100.0;

        List<ResenaResponse> ultimas = resenas.stream()
                .sorted(Comparator.comparing(Resena::getFecha).reversed())
                .limit(3)
                .map(ResenaResponse::from)
                .toList();

        return new Reputacion(promedio, resenas.size(), dias, ultimas);
    }

    /**
     * Pre : los productos y como medirlos.
     * Post: el que va primero segun esa medida, o null si no hay ninguno o si
     *       todos estan en cero: destacar un producto con cero ventas seria
     *       decir algo que no significa nada.
     */
    private ProductoDestacado destacado(List<Producto> productos,
            java.util.function.ToLongFunction<Producto> medida) {
        return productos.stream()
                .max(Comparator.comparingLong(medida))
                .filter(p -> medida.applyAsLong(p) > 0)
                .map(p -> new ProductoDestacado(p.getId(), p.getNombre(), medida.applyAsLong(p)))
                .orElse(null);
    }

    private MetodoEntrega metodoDe(Envio envio) {
        return envio.getOrden() == null || envio.getOrden().getMetodoEntrega() == null
                ? MetodoEntrega.DESPACHO
                : envio.getOrden().getMetodoEntrega();
    }
}
