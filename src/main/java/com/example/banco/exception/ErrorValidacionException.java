package com.example.banco.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

/** Datos que no cumplen una regla de validación de negocio. */
public class ErrorValidacionException extends NegocioException {

    public ErrorValidacionException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, "ERROR_VALIDACION", mensaje);
    }

    public ErrorValidacionException(String mensaje, List<ErrorCampo> errores) {
        super(HttpStatus.BAD_REQUEST, "ERROR_VALIDACION", mensaje, errores);
    }

    public static ErrorValidacionException campo(String campo, String mensaje) {
        return new ErrorValidacionException(campo + ": " + mensaje, List.of(new ErrorCampo(campo, mensaje)));
    }
}
