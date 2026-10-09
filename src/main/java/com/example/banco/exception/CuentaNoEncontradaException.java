package com.example.banco.exception;

import org.springframework.http.HttpStatus;

/** No existe una cuenta con ese número. */
public class CuentaNoEncontradaException extends NegocioException {

    public CuentaNoEncontradaException(String numeroCuenta) {
        super(HttpStatus.NOT_FOUND, "CUENTA_NO_ENCONTRADA", "No existe una cuenta con el número " + numeroCuenta);
    }
}
