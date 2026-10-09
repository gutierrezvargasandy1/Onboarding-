package com.example.banco.exception;

import lombok.Getter;

/**
 * Valor de catálogo inexistente en el JSON (por ejemplo sexo = "Z"). Se lanza
 * mientras Jackson lee la petición; el manejador global la convierte en un 400 claro.
 */
@Getter
public class ValorCatalogoInvalidoException extends IllegalArgumentException {

    private final String campo;
    private final String permitidos;

    public ValorCatalogoInvalidoException(String campo, String valor, String permitidos) {
        super("Valor '" + valor + "' no válido para " + campo + ". Valores permitidos: " + permitidos);
        this.campo = campo;
        this.permitidos = permitidos;
    }
}
