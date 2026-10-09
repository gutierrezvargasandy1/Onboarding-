package com.example.banco.integracion;

import com.example.banco.soporte.PruebaIntegracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("API · Baja lógica (DELETE /clientes/{id})")
class BajaLogicaIT extends PruebaIntegracion {

    private ClienteRegistrado cliente;
    private Sesion sesionDelCliente;
    private String tokenEjecutivo;

    @BeforeEach
    void preparar() throws Exception {
        cliente = registrarCliente();
        sesionDelCliente = iniciarSesion(cliente);
        tokenEjecutivo = sesionNueva().token();
    }

    private ResultActions darDeBaja(Integer id) throws Exception {
        return mvc.perform(delete("/clientes/" + id).header(HttpHeaders.AUTHORIZATION, bearer(tokenEjecutivo)));
    }

    @Test
    @DisplayName("Desactiva al cliente sin borrarlo: activo = false y fecha de baja registrada")
    void desactivaSinBorrar() throws Exception {
        darDeBaja(cliente.id()).andExpect(status().isNoContent());

        Map<String, Object> fila = jdbc.queryForMap("SELECT activo, fecha_baja FROM onboarding.clientes WHERE id = ?", cliente.id());
        assertThat(fila.get("activo")).isEqualTo(false);
        assertThat(fila.get("fecha_baja")).isNotNull();
        assertThat(contarFilas("clientes")).isEqualTo(2);
        assertThat(contarFilas("domicilios")).isEqualTo(2);
        assertThat(contarFilas("cuentas")).isEqualTo(2);

        getAutenticado("/clientes/" + cliente.id(), tokenEjecutivo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.fechaBaja").isNotEmpty());
    }

    @Test
    @DisplayName("Sus cuentas activas pasan a INACTIVA (solo clientes activos tienen cuentas activas)")
    void inactivaCuentas() throws Exception {
        darDeBaja(cliente.id()).andExpect(status().isNoContent());

        getAutenticado("/cuentas/" + cliente.numeroCuenta(), tokenEjecutivo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estatus").value("INACTIVA"));
        getAutenticado("/cuentas/activas", tokenEjecutivo)
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    @DisplayName("Su usuario queda inactivo: ya no puede iniciar sesión y su token deja de servir")
    void inactivaUsuario() throws Exception {
        darDeBaja(cliente.id()).andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("SELECT activo FROM onboarding.usuarios WHERE cliente_id = ?", Boolean.class, cliente.id()))
                .isFalse();
        login(cliente.correo(), cliente.password())
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("USUARIO_INACTIVO"));
        getAutenticado("/clientes", sesionDelCliente.token())
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("TOKEN_RECHAZADO"));
    }

    @Test
    @DisplayName("Repetir la baja es idempotente (204) y un cliente inexistente da 404")
    void idempotenteEInexistente() throws Exception {
        darDeBaja(cliente.id()).andExpect(status().isNoContent());
        darDeBaja(cliente.id()).andExpect(status().isNoContent());
        darDeBaja(999).andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("CLIENTE_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("Ni siquiera el dueño de la base de datos puede borrar físicamente un cliente")
    void sinBorradoFisico() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        jdbc.update("DELETE FROM onboarding.clientes WHERE id = ?", cliente.id()))
                .hasMessageContaining("la baja es lógica");
        assertThat(contarFilas("clientes")).isEqualTo(2);
    }
}
