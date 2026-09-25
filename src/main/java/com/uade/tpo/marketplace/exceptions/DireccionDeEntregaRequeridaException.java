package com.uade.tpo.marketplace.exceptions;

import org.springframework.http.HttpStatus;

public class DireccionDeEntregaRequeridaException extends ExcepcionDeNegocio {
    public DireccionDeEntregaRequeridaException() {
        super(HttpStatus.BAD_REQUEST, "Hay que indicar a donde se despacha: provincia, "
                + "localidad y calle. Si preferis coordinar con el vendedor, manda "
                + "coordinarConVendedor en true");
    }
}
