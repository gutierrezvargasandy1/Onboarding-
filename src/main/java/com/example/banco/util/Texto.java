package com.example.banco.util;

import java.util.Locale;
import java.util.regex.Pattern;

/** Normalización de texto capturado (se aplica antes de validar). */
public final class Texto {

    private static final Pattern ESPACIOS = Pattern.compile("\\s+");

    private Texto() {
    }

    /** Quita espacios al inicio y al final, deja uno solo entre palabras; vacío se vuelve null. */
    public static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = ESPACIOS.matcher(valor.strip()).replaceAll(" ");
        return limpio.isEmpty() ? null : limpio;
    }

    public static String mayusculas(String valor) {
        String limpio = limpiar(valor);
        return limpio == null ? null : limpio.toUpperCase(Locale.ROOT);
    }

    public static String minusculas(String valor) {
        String limpio = limpiar(valor);
        return limpio == null ? null : limpio.toLowerCase(Locale.ROOT);
    }
}
