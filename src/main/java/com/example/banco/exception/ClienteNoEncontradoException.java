package com.example.banco.exception;

import org.springframework.http.HttpStatus;

/** No existe un cliente con el criterio buscado. */
public class ClienteNoEncontradoException extends NegocioException {

    public ClienteNoEncontradoException(String detalle) {
        super(HttpStatus.NOT_FOUND, "CLIENTE_NO_ENCONTRADO", detalle);
    }

    public static ClienteNoEncontradoException porId(Integer id) {
        return new ClienteNoEncontradoException("No existe un cliente con id " + id);
    }
}
