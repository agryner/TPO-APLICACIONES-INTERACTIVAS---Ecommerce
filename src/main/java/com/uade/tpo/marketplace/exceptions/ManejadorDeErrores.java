package com.uade.tpo.marketplace.exceptions;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Ultima red antes de que un error salga por HTTP.
 *
 * Extiende ResponseEntityExceptionHandler para no pisar el trabajo que Spring
 * ya hace con sus propias excepciones: un JSON malformado, un enum invalido o
 * un metodo equivocado siguen saliendo con 400 y 405 como corresponde.
 *
 * Las excepciones de dominio tampoco pasan por aca: cada una lleva su
 * @ResponseStatus y Spring las traduce sola. Esto atrapa lo que quedaba
 * afuera, que es lo que salia como 500 con el SQL adentro.
 *
 * La regla es siempre la misma: al cliente se le dice que paso en su idioma, y
 * el detalle tecnico va al log del servidor y no al JSON.
 */
@RestControllerAdvice
public class ManejadorDeErrores extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorDeErrores.class);

    /**
     * Body que no cumple las anotaciones de los Request.
     *
     * Se sobreescribe el metodo de Spring para agregar la lista de campos con
     * su motivo: un "400 Bad Request" pelado obliga a adivinar cual de los
     * siete campos estaba mal.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> campos.putIfAbsent(
                e.getField(),
                e.getDefaultMessage() == null ? "valor invalido" : e.getDefaultMessage()));

        Map<String, Object> cuerpo = base(HttpStatus.BAD_REQUEST, "Hay datos invalidos en el pedido");
        cuerpo.put("campos", campos);
        return ResponseEntity.badRequest().body(cuerpo);
    }

    /**
     * Choques con la base: un unique repetido, un dato mas largo que la columna.
     *
     * El mensaje de Hibernate nombra tablas, columnas y constraints, asi que no
     * se reenvia nunca: queda en el log y el cliente recibe un texto neutro.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> integridad(DataIntegrityViolationException ex) {
        log.warn("Violacion de integridad", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(base(HttpStatus.CONFLICT,
                        "Los datos enviados chocan con algo que ya existe o no entran en el campo"));
    }

    /**
     * Dos operaciones que se pisaron sobre las mismas filas.
     *
     * Con los candados del checkout deberia ser raro, pero si igual pasa no es
     * culpa de quien pidio: es un choque momentaneo y reintentar alcanza. Por
     * eso 409 y no 500.
     */
    @ExceptionHandler({ CannotAcquireLockException.class, PessimisticLockingFailureException.class })
    public ResponseEntity<Object> choqueDeConcurrencia(Exception ex) {
        log.warn("Choque de concurrencia", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(base(HttpStatus.CONFLICT,
                        "Otra operacion esta usando esos datos en este momento, volve a intentar"));
    }

    /** Lo que no previo nadie. Nunca sale con detalle al cliente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> inesperado(Exception ex) throws Exception {
        // Guarda imprescindible: sin esto, este metodo se come tambien las 21
        // excepciones de dominio y las devuelve como 500, tirando a la basura
        // los 403, 404 y 409 que cada una declara en su @ResponseStatus.
        if (AnnotatedElementUtils.hasAnnotation(ex.getClass(), ResponseStatus.class))
            throw ex;

        log.error("Error no contemplado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(base(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error inesperado"));
    }

    private Map<String, Object> base(HttpStatus estado, String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("timestamp", Instant.now().toString());
        cuerpo.put("status", estado.value());
        cuerpo.put("error", estado.getReasonPhrase());
        cuerpo.put("message", mensaje);
        return cuerpo;
    }
}
