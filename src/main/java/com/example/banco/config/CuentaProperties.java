package com.example.banco.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * Parámetros de apertura de cuenta. El saldo inicial lo define el sistema (no el
 * cliente) y se valida al arrancar: si fuera negativo, la aplicación no inicia.
 */
@Validated
@ConfigurationProperties(prefix = "app.cuenta")
public record CuentaProperties(
        @NotNull
        @DecimalMin(value = "0.00", message = "El saldo inicial no puede ser negativo")
        @Digits(integer = 12, fraction = 2)
        BigDecimal saldoInicial,

        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "Moneda ISO 4217 de 3 letras")
        String moneda) {
}
