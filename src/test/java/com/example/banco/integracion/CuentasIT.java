package com.example.banco.integracion;

import com.example.banco.soporte.PruebaIntegracion;
import com.example.banco.util.NumeroCuenta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("API · Cuentas (GET /cuentas)")
class CuentasIT extends PruebaIntegracion {

    private ClienteRegistrado cliente;
    private String token;

    @BeforeEach
    void preparar() throws Exception {
        cliente = registrarCliente();
        token = iniciarSesion(cliente).token();
    }

    @Test
    @DisplayName("Consulta la cuenta por número: asociada al cliente, ACTIVA, MXN")
    void porNumero() throws Exception {
        getAutenticado("/cuentas/" + cliente.numeroCuenta(), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroCuenta").value(cliente.numeroCuenta()))
                .andExpect(jsonPath("$.clienteId").value(cliente.id()))
                .andExpect(jsonPath("$.estatus").value("ACTIVA"))
                .andExpect(jsonPath("$.moneda").value("MXN"))
                .andExpect(jsonPath("$.fechaApertura").isNotEmpty());
    }

    @Test
    @DisplayName("Consulta el saldo de la cuenta (saldo inicial definido por el sistema)")
    void saldo() throws Exception {
        getAutenticado("/cuentas/" + cliente.numeroCuenta() + "/saldo", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(1000.00))
                .andExpect(jsonPath("$.moneda").value("MXN"))
                .andExpect(jsonPath("$.estatus").value("ACTIVA"))
                .andExpect(jsonPath("$.fechaConsulta").isNotEmpty());
    }

    @Test
    @DisplayName("Cuenta inexistente o con dígito verificador incorrecto: 404 CUENTA_NO_ENCONTRADA")
    void noEncontrada() throws Exception {
        getAutenticado("/cuentas/1234567897", token)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("CUENTA_NO_ENCONTRADA"));
        getAutenticado("/cuentas/1000000010", token)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("CUENTA_NO_ENCONTRADA"));
    }

    @Test
    @DisplayName("Número con formato inválido: 400")
    void formatoInvalido() throws Exception {
        getAutenticado("/cuentas/123", token).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("numeroCuenta"));
        getAutenticado("/cuentas/ABCDEFGHIJ/saldo", token).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cuentas activas paginadas")
    void activas() throws Exception {
        registrarCliente();
        getAutenticado("/cuentas/activas?size=1", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].estatus").value("ACTIVA"));
        getAutenticado("/cuentas/activas?sort=saldo,desc", token).andExpect(status().isOk());
        getAutenticado("/cuentas/activas?sort=clienteId", token).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cada cliente recibe un número de cuenta único de 10 dígitos con verificador válido")
    void numerosUnicos() throws Exception {
        Set<String> numeros = new HashSet<>();
        numeros.add(cliente.numeroCuenta());
        for (int i = 0; i < 9; i++) {
            numeros.add(registrarCliente().numeroCuenta());
        }
        assertThat(numeros).hasSize(10).allSatisfy(numero -> assertThat(NumeroCuenta.esValido(numero)).isTrue());
    }
}
