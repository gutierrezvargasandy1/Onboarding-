package com.example.banco.util;

import java.util.regex.Pattern;

/**
 * Número de cuenta: 10 dígitos = 9 de la secuencia de la base de datos + 1 dígito
 * verificador Luhn (el mismo algoritmo de las tarjetas bancarias). El dígito
 * detecta errores de captura sin consultar la base de datos.
 */
public final class NumeroCuenta {

    private static final Pattern FORMATO = Pattern.compile(Constantes.REGEX_NUMERO_CUENTA);

    private NumeroCuenta() {
    }

    public static boolean tieneFormato(String numero) {
        return numero != null && FORMATO.matcher(numero).matches();
    }

    /** Formato correcto y dígito verificador correcto. */
    public static boolean esValido(String numero) {
        return tieneFormato(numero) && digitoLuhn(numero.substring(0, 9)) == numero.charAt(9) - '0';
    }

    /** Dígito verificador Luhn de una cadena de dígitos (se duplica desde el dígito más a la derecha). */
    public static int digitoLuhn(String digitos) {
        int suma = 0;
        boolean duplicar = true;
        for (int i = digitos.length() - 1; i >= 0; i--) {
            int valor = digitos.charAt(i) - '0';
            if (valor < 0 || valor > 9) {
                throw new IllegalArgumentException("Solo se admiten dígitos");
            }
            if (duplicar) {
                valor *= 2;
                if (valor > 9) {
                    valor -= 9;
                }
            }
            suma += valor;
            duplicar = !duplicar;
        }
        return (10 - suma % 10) % 10;
    }

    /** Para logs: solo muestra los últimos 4 dígitos. */
    public static String enmascarar(String numero) {
        if (numero == null || numero.length() < 4) {
            return "****";
        }
        return "******" + numero.substring(numero.length() - 4);
    }
}
