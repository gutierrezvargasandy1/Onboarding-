package com.example.banco.integracion;

import com.example.banco.soporte.DatosPrueba;
import com.example.banco.soporte.PruebaIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El alta es una sola transacción: si falla el último paso (crear el usuario),
 * tampoco quedan guardados el cliente, su domicilio ni su cuenta.
 */
@DisplayName("Registro atómico: todo o nada")
class AtomicidadRegistroIT extends PruebaIntegracion {

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Si falla la creación del usuario no queda ni cliente, ni domicilio, ni cuenta")
    void todoONada() throws Exception {
        when(passwordEncoder.encode(any())).thenThrow(new IllegalStateException("falla simulada al cifrar"));

        registrar(DatosPrueba.cliente())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("falla simulada"))));

        assertThat(contarFilas("clientes")).isZero();
        assertThat(contarFilas("domicilios")).isZero();
        assertThat(contarFilas("cuentas")).isZero();
        assertThat(contarFilas("usuarios")).isZero();
    }
}
