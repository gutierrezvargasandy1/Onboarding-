package com.example.banco.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Normalización de texto")
class TextoTest {

    @Test
    @DisplayName("Quita espacios sobrantes y deja uno solo entre palabras")
    void limpia() {
        assertThat(Texto.limpiar("  María   Fernanda ")).isEqualTo("María Fernanda");
        assertThat(Texto.limpiar("   ")).isNull();
        assertThat(Texto.limpiar(null)).isNull();
    }

    @Test
    @DisplayName("Convierte a mayúsculas o minúsculas sin depender del idioma del sistema")
    void mayusculasYMinusculas() {
        assertThat(Texto.mayusculas(" lohf950821mgtprr08 ")).isEqualTo("LOHF950821MGTPRR08");
        assertThat(Texto.mayusculas("ñuño")).isEqualTo("ÑUÑO");
        assertThat(Texto.minusculas(" Maria.Lopez@Ejemplo.COM ")).isEqualTo("maria.lopez@ejemplo.com");
        assertThat(Texto.mayusculas(null)).isNull();
        assertThat(Texto.minusculas(null)).isNull();
    }
}
