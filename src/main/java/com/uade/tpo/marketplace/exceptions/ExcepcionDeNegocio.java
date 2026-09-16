package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public abstract class ExcepcionDeNegocio extends Exception {
    private final HttpStatus estado;

    /**
     * Pre : el codigo HTTP que le corresponde a esta falla y el mensaje para
     *       quien la provoco.
     * Post: la excepcion armada. El codigo viaja adentro y no en una
     *       anotacion, asi lo lee ManejadorDeErrores sin reflexion y el
     *       mensaje no lo pisa nadie.
     */
    protected ExcepcionDeNegocio(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }
}
