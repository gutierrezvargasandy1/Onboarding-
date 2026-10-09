package com.example.banco.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Número de cuenta con dígito verificador Luhn")
class NumeroCuentaTest {

    @Test
    @DisplayName("Calcula el dígito Luhn de referencia (7992739871 -> 3)")
    void digitoDeReferencia() {
        assertThat(NumeroCuenta.digitoLuhn("7992739871")).isEqualTo(3);
    }

    @Test
    @DisplayName("Coincide con la función de la base de datos para los primeros números de la secuencia")
    void coincideConLaBaseDeDatos() {
        // fn_generar_numero_cuenta(): 100000001 -> 1000000016, 100000002 -> 1000000024
        assertThat("100000001" + NumeroCuenta.digitoLuhn("100000001")).isEqualTo("1000000016");
        assertThat("100000002" + NumeroCuenta.digitoLuhn("100000002")).isEqualTo("1000000024");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1000000016", "1000000024", "1234567897", "9999999999"})
    @DisplayName("Acepta números de 10 dígitos con verificador correcto")
    void aceptaNumerosValidos(String numero) {
        assertThat(NumeroCuenta.tieneFormato(numero)).isTrue();
        assertThat(NumeroCuenta.esValido(numero)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1000000010", "1000000019", "1000000061"})
    @DisplayName("Detecta un dígito verificador incorrecto o dígitos transpuestos")
    void detectaErroresDeCaptura(String numero) {
        assertThat(NumeroCuenta.tieneFormato(numero)).isTrue();
        assertThat(NumeroCuenta.esValido(numero)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123456789", "12345678901", "10000000AB", "1000 00001"})
    @DisplayName("Rechaza formatos distintos de 10 dígitos")
    void rechazaFormatos(String numero) {
        assertThat(NumeroCuenta.tieneFormato(numero)).isFalse();
        assertThat(NumeroCuenta.esValido(numero)).isFalse();
    }

    @Test
    @DisplayName("Rechaza nulos y caracteres no numéricos")
    void rechazaNulos() {
        assertThat(NumeroCuenta.esValido(null)).isFalse();
        assertThatThrownBy(() -> NumeroCuenta.digitoLuhn("12a")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Enmascara el número para los logs")
    void enmascara() {
        assertThat(NumeroCuenta.enmascarar("1000000016")).isEqualTo("******0016");
        assertThat(NumeroCuenta.enmascarar(null)).isEqualTo("****");
    }
}
