package com.example.banco.exception;

import org.springframework.http.HttpStatus;

/** El usuario autenticado intenta operar sobre un recurso que no es suyo. */
public class AccesoDenegadoException extends NegocioException {

    public AccesoDenegadoException(String mensaje) {
        super(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", mensaje);
    }
}
