package com.example.banco.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Coherencia entre la fecha de nacimiento y los documentos oficiales: la CURP
 * (posiciones 5 a 10) y el RFC de persona física (13 caracteres, posiciones 5 a 10)
 * contienen la fecha de nacimiento en formato AAMMDD.
 */
public final class DocumentosIdentidad {

    private static final DateTimeFormatter AAMMDD = DateTimeFormatter.ofPattern("yyMMdd");

    private DocumentosIdentidad() {
    }

    public static String fechaAammdd(LocalDate fecha) {
        return fecha.format(AAMMDD);
    }

    public static boolean fechaCoincideConCurp(String curp, LocalDate fechaNacimiento) {
        if (curp == null || fechaNacimiento == null || curp.length() != 18) {
            return false;
        }
        return curp.substring(4, 10).equals(fechaAammdd(fechaNacimiento));
    }

    /** El RFC de 12 caracteres (persona moral) no lleva fecha de nacimiento: no se compara. */
    public static boolean fechaCoincideConRfc(String rfc, LocalDate fechaNacimiento) {
        if (rfc == null || fechaNacimiento == null) {
            return false;
        }
        if (rfc.length() == 12) {
            return true;
        }
        return rfc.length() == 13 && rfc.substring(4, 10).equals(fechaAammdd(fechaNacimiento));
    }
}
