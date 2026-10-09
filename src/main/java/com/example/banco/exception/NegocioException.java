package com.example.banco.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Base de las excepciones de negocio. Cada una define su estatus HTTP y un código
 * estable (por ejemplo CURP_DUPLICADA) que el cliente de la API puede interpretar.
 */
@Getter
public abstract class NegocioException extends RuntimeException {

    private final HttpStatus estatus;
    private final String codigo;
    private final transient List<ErrorCampo> errores;

    protected NegocioException(HttpStatus estatus, String codigo, String mensaje) {
        this(estatus, codigo, mensaje, List.of());
    }

    protected NegocioException(HttpStatus estatus, String codigo, String mensaje, List<ErrorCampo> errores) {
        super(mensaje);
        this.estatus = estatus;
        this.codigo = codigo;
        this.errores = List.copyOf(errores);
    }
}
