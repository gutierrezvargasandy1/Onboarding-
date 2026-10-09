package com.example.banco.exception;

import org.springframework.http.HttpStatus;

/** La contraseña no cumple la política o la contraseña actual no coincide. */
public class PasswordInvalidaException extends NegocioException {

    public PasswordInvalidaException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, "PASSWORD_INVALIDA", mensaje);
    }
}
