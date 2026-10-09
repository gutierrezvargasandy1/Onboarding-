package com.example.banco.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

/** Intento de cambiar CURP, RFC o número de cuenta. */
public class CampoNoModificableException extends NegocioException {

    public CampoNoModificableException(String campo, String nombreLegible) {
        super(HttpStatus.BAD_REQUEST, "CAMPO_NO_MODIFICABLE", nombreLegible + " no puede modificarse",
                List.of(new ErrorCampo(campo, "no puede modificarse")));
    }
}
