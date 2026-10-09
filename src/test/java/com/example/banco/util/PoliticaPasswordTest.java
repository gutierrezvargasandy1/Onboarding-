package com.example.banco.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Política de contraseñas")
class PoliticaPasswordTest {

    @ParameterizedTest(name = "\"{0}\" cumple la política")
    @ValueSource(strings = {"Segura#2026", "Abcdef1!", "Ñandú#2026", "Contraseña muy larga 1!"})
    @DisplayName("Acepta contraseñas con 8+ caracteres, mayúscula, minúscula, número y especial")
    void aceptaContrasenasSeguras(String password) {
        assertThat(PoliticaPassword.incumplimientos(password)).isEmpty();
        assertThat(PoliticaPassword.cumple(password)).isTrue();
    }

    @ParameterizedTest(name = "\"{0}\" -> falta: {1}")
    @CsvSource(delimiter = '|', value = {
            "Ab1!xyz        | mínimo 8 caracteres",
            "segura#2026    | al menos una letra mayúscula",
            "SEGURA#2026    | al menos una letra minúscula",
            "Segura#Clave   | al menos un número",
            "Segura2026     | al menos un carácter especial"
    })
    @DisplayName("Indica exactamente qué regla no se cumple")
    void indicaLaReglaIncumplida(String password, String regla) {
        assertThat(PoliticaPassword.incumplimientos(password)).containsExactly(regla);
    }

    @Test
    @DisplayName("Rechaza contraseñas de más de 72 bytes (límite de BCrypt)")
    void rechazaMasDe72Bytes() {
        String larga = "Aa1!" + "x".repeat(69);
        assertThat(PoliticaPassword.incumplimientos(larga)).containsExactly("máximo 72 bytes");
    }

    @Test
    @DisplayName("Contraseña nula o vacía es obligatoria")
    void nulaOVacia() {
        assertThat(PoliticaPassword.incumplimientos(null)).containsExactly("la contraseña es obligatoria");
        assertThat(PoliticaPassword.incumplimientos("")).containsExactly("la contraseña es obligatoria");
    }

    @Test
    @DisplayName("Una contraseña débil acumula todos sus incumplimientos")
    void acumulaIncumplimientos() {
        assertThat(PoliticaPassword.incumplimientos("abc"))
                .containsExactly("mínimo 8 caracteres", "al menos una letra mayúscula",
                        "al menos un número", "al menos un carácter especial");
    }
}
