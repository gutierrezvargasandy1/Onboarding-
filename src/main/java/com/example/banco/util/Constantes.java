package com.example.banco.util;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Constantes de negocio. Las expresiones regulares son las mismas que usan los
 * CHECK de la base de datos, así que Java y PostgreSQL validan exactamente igual.
 */
public final class Constantes {

    private Constantes() {
    }

    public static final ZoneId ZONA_HORARIA = ZoneId.of("America/Mexico_City");

    public static final int EDAD_MINIMA = 18;
    public static final LocalDate FECHA_NACIMIENTO_MINIMA = LocalDate.of(1900, 1, 1);

    /** El catálogo de estados (INEGI) y el CP de 5 dígitos son de México. */
    public static final String PAIS_DOMICILIO = "MX";

    /** Letras latinas, incluidas las acentuadas, con diéresis y la ñ (bloque Latin-1). */
    public static final String LETRAS = "A-Za-zÀ-ÖØ-öø-ÿ";

    public static final String REGEX_NOMBRE = "^[" + LETRAS + "]+( [" + LETRAS + "]+)*$";

    public static final String REGEX_CURP =
            "^[A-Z][AEIOUX][A-Z]{2}[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[HMX]"
            + "(AS|BC|BS|CC|CL|CM|CS|CH|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QT|QR|SP|SL|SR|TC|TS|TL|VZ|YN|ZS|NE)"
            + "[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9][0-9]$";

    /** 13 caracteres (persona física) o 12 (persona moral). */
    public static final String REGEX_RFC =
            "^[A-ZÑ&]{3,4}[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[A-Z0-9]{2}[0-9A]$";

    public static final String REGEX_CORREO =
            "^[a-z0-9_%+-]+(\\.[a-z0-9_%+-]+)*@([a-z0-9]([a-z0-9-]*[a-z0-9])?\\.)+[a-z]{2,}$";

    public static final String REGEX_TELEFONO = "^[0-9]{10}$";
    public static final String REGEX_CODIGO_POSTAL = "^[0-9]{5}$";
    public static final String REGEX_CODIGO_PAIS = "^[A-Z]{2}$";
    public static final String REGEX_NUMERO_CUENTA = "^[0-9]{10}$";

    public static final String CAMPO_OBLIGATORIO = "campo obligatorio";
}
