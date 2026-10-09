package com.example.banco.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Política de contraseñas de la tarea: mínimo 8 caracteres, una mayúscula, una
 * minúscula, un número y un carácter especial. Además, máximo 72 bytes, que es
 * lo que BCrypt alcanza a procesar.
 */
public final class PoliticaPassword {

    public static final int LONGITUD_MINIMA = 8;
    public static final int BYTES_MAXIMOS = 72;

    private PoliticaPassword() {
    }

    /** Devuelve lo que le falta a la contraseña; lista vacía si cumple. */
    public static List<String> incumplimientos(String password) {
        List<String> faltantes = new ArrayList<>();
        if (password == null || password.isEmpty()) {
            faltantes.add("la contraseña es obligatoria");
            return faltantes;
        }
        if (password.length() < LONGITUD_MINIMA) {
            faltantes.add("mínimo " + LONGITUD_MINIMA + " caracteres");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            faltantes.add("al menos una letra mayúscula");
        }
        if (password.chars().noneMatch(Character::isLowerCase)) {
            faltantes.add("al menos una letra minúscula");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            faltantes.add("al menos un número");
        }
        if (password.chars().noneMatch(PoliticaPassword::esEspecial)) {
            faltantes.add("al menos un carácter especial");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > BYTES_MAXIMOS) {
            faltantes.add("máximo " + BYTES_MAXIMOS + " bytes");
        }
        return faltantes;
    }

    public static boolean cumple(String password) {
        return incumplimientos(password).isEmpty();
    }

    private static boolean esEspecial(int caracter) {
        return !Character.isLetterOrDigit(caracter) && !Character.isWhitespace(caracter);
    }
}
