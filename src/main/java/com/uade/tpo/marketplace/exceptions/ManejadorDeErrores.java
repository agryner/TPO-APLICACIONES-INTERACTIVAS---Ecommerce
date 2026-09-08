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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ManejadorDeErrores extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ManejadorDeErrores.class);

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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> integridad(DataIntegrityViolationException ex) {
        log.warn("Violacion de integridad", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(base(HttpStatus.CONFLICT,
                        "Los datos enviados chocan con algo que ya existe o no entran en el campo"));
    }

    @ExceptionHandler({ CannotAcquireLockException.class, PessimisticLockingFailureException.class })
    public ResponseEntity<Object> choqueDeConcurrencia(Exception ex) {
        log.warn("Choque de concurrencia", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(base(HttpStatus.CONFLICT,
                        "Otra operacion esta usando esos datos en este momento, volve a intentar"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> credencialesInvalidas(AuthenticationException ex) {
        log.debug("Autenticacion fallida", ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(base(HttpStatus.UNAUTHORIZED, "Mail o contrasena incorrectos"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> sinPermiso(AccessDeniedException ex) {
        log.debug("Acceso denegado", ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(base(HttpStatus.FORBIDDEN, "No tenes permiso para hacer eso"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> inesperado(Exception ex) throws Exception {
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
