package com.uade.tpo.marketplace.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.uade.tpo.marketplace.entity.Categoria;
import com.uade.tpo.marketplace.repository.CategoriaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Le pregunta a Gemini si una foto se corresponde con la categoria que declaro
 * el vendedor.
 *
 * Lo llama FotoServiceImpl antes de guardar la imagen. De la base solo lee las
 * categorias con las que arma el prompt: como salen de ahi y no de una lista
 * fija en el codigo, una categoria nueva queda cubierta sola.
 */
@Service
@RequiredArgsConstructor
public class VerificadorImagenService {

    /** Lo que contesta el modelo, con los mismos campos del verificador original. */
    public record Resultado(boolean coincide, double confianza, String queVeo,
            String categoriaSugerida, String mensajeAlVendedor) {

        /**
         * Puntaje unico de 0 a 1: que tan probable es que la foto corresponda.
         *
         * El modelo devuelve la confianza en SU veredicto, no la probabilidad de
         * que coincida: un "no corresponde" con confianza 0.99 significa que
         * esta segurisimo de que la foto esta mal. Invirtiendolo cuando coincide
         * es false, el numero se lee siempre igual y los umbrales tienen sentido
         * en las dos direcciones.
         */
        public double puntaje() {
            return coincide ? confianza : 1 - confianza;
        }
    }

    private static final String RECHAZOS_TIPICOS = "selfie o retrato, interior de vivienda, auto o "
            + "moto particular, captura de pantalla, comida, ropa, celular o notebook, mascota, "
            + "documento escaneado, o imagen tan borrosa que no se reconozca nada";

    /** Las APIs reescalan igual: mandarla en 12MP solo agrega latencia. */
    private static final int LADO_MAX = 1024;

    private final CategoriaRepository categoriaRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${marketplace.ia.api-key}")
    private String apiKey;

    @Value("${marketplace.ia.modelo}")
    private String modelo;

    @Value("${marketplace.ia.timeout-segundos:20}")
    private int timeoutSegundos;

    public Resultado verificar(byte[] imagen, Categoria categoria) throws Exception {
        byte[] jpeg = preparar(imagen);

        Map<String, Object> cuerpo = Map.of(
                "contents", List.of(Map.of("parts", List.of(
                        Map.of("inline_data", Map.of(
                                "mime_type", "image/jpeg",
                                "data", Base64.getEncoder().encodeToString(jpeg))),
                        Map.of("text", construirPrompt(categoria))))),
                "generationConfig", Map.of("response_mime_type", "application/json"));

        String respuesta = cliente()
                .post()
                .uri("/v1beta/models/{modelo}:generateContent?key={key}", modelo, apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .body(String.class);

        JsonNode raiz = mapper.readTree(respuesta);
        String texto = raiz.path("candidates").path(0).path("content").path("parts").path(0)
                .path("text").asText();

        JsonNode d = mapper.readTree(limpiar(texto));
        return new Resultado(
                d.path("coincide").asBoolean(),
                d.path("confianza").asDouble(),
                textoODefault(d, "que_veo"),
                textoODefault(d, "categoria_sugerida"),
                textoODefault(d, "mensaje_al_vendedor"));
    }

    private RestClient cliente() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(timeoutSegundos));
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));
        return RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .requestFactory(fabrica)
                .build();
    }

    /**
     * El prompt se arma con la rama de la categoria declarada y los nombres del
     * resto, las dos cosas leidas de la base. Por eso no hay taxonomia fija: si
     * el admin crea una categoria, la siguiente verificacion ya la contempla.
     */
    private String construirPrompt(Categoria categoria) {
        List<String> rama = new ArrayList<>();
        for (Categoria actual = categoria; actual != null; actual = actual.getCategoriaPadre())
            rama.add(0, actual.getDescripcion() == null || actual.getDescripcion().isBlank()
                    ? actual.getNombre()
                    : actual.getNombre() + " (" + actual.getDescripcion() + ")");

        String otras = categoriaRepository.findAll().stream()
                .map(Categoria::getNombre)
                .filter(n -> !n.equals(categoria.getNombre()))
                .reduce((a, b) -> a + ", " + b)
                .orElse("no hay otras");

        return PLANTILLA.formatted(categoria.getNombre(), String.join(" > ", rama), otras,
                RECHAZOS_TIPICOS);
    }

    private static final String PLANTILLA = """
            Sos el verificador de imagenes de un marketplace argentino de insumos y \
            maquinaria agropecuaria.

            El vendedor ya eligio la categoria "%s" y ahora subio una foto. Tu unica tarea \
            es decidir si la foto se corresponde con esa categoria.

            UBICACION EN EL ARBOL DE CATEGORIAS: %s
            OTRAS CATEGORIAS DEL SITIO: %s

            Tampoco corresponde nada que sea: %s.

            Criterios:
            - Fotos oscuras, sucias, sacadas en galpon o a contraluz son NORMALES en este \
            rubro. No rechaces por calidad salvo que sea imposible reconocer el objeto.
            - Una foto de detalle o de una parte del producto es valida.
            - Si hay una persona junto al producto, no importa: mira el producto.
            - Si la foto es del rubro agro pero de OTRA categoria, coincide=false y pone cual \
            creas que es en categoria_sugerida.

            En confianza va que tan seguro estas de TU VEREDICTO, de 0.0 a 1.0.

            Responde UNICAMENTE con este JSON, sin texto adicional ni backticks:
            {"coincide": true|false,
             "confianza": 0.0-1.0,
             "que_veo": "una frase corta",
             "categoria_sugerida": "nombre de categoria o null",
             "mensaje_al_vendedor": "explicacion breve y amable, o null si coincide"}""";

    /** Baja la resolucion de la foto antes de mandarla y la normaliza a JPEG. */
    private byte[] preparar(byte[] original) throws Exception {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(original));
        if (img == null)
            throw new IllegalArgumentException("No se pudo leer la imagen");

        int ancho = img.getWidth();
        int alto = img.getHeight();
        double escala = Math.min(1.0, (double) LADO_MAX / Math.max(ancho, alto));
        int nuevoAncho = Math.max(1, (int) Math.round(ancho * escala));
        int nuevoAlto = Math.max(1, (int) Math.round(alto * escala));

        BufferedImage salida = new BufferedImage(nuevoAncho, nuevoAlto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = salida.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, nuevoAncho, nuevoAlto, null);
        g.dispose();

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ImageIO.write(salida, "jpg", buffer);
        return buffer.toByteArray();
    }

    /** El modelo manda el literal null como texto cuando el campo no aplica. */
    private String textoODefault(JsonNode d, String campo) {
        JsonNode nodo = d.path(campo);
        if (nodo.isMissingNode() || nodo.isNull())
            return null;

        String valor = nodo.asText();
        return valor == null || valor.isBlank() || "null".equals(valor) ? null : valor;
    }

    /** A veces el modelo envuelve el JSON en un bloque de codigo. */
    private String limpiar(String texto) {
        String limpio = texto.strip();
        if (limpio.startsWith("```json"))
            limpio = limpio.substring(7);
        else if (limpio.startsWith("```"))
            limpio = limpio.substring(3);
        if (limpio.endsWith("```"))
            limpio = limpio.substring(0, limpio.length() - 3);
        return limpio.strip();
    }
}
