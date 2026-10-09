package com.example.banco.integracion;

import com.example.banco.soporte.PruebaIntegracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("API · Usuarios (GET /usuarios/{id} y PUT /usuarios/{id}/password)")
class UsuariosIT extends PruebaIntegracion {

    private static final String NUEVA = "NuevaClave#2026";

    private ClienteRegistrado cliente;
    private Sesion sesion;

    @BeforeEach
    void preparar() throws Exception {
        cliente = registrarCliente();
        sesion = iniciarSesion(cliente);
    }

    private ResultActions cambiarPassword(String token, Integer usuarioId, String actual, String nueva) throws Exception {
        return putAutenticado("/usuarios/" + usuarioId + "/password", token,
                Map.of("passwordActual", actual, "passwordNueva", nueva));
    }

    @Test
    @DisplayName("Consulta su propio usuario sin la contraseña")
    void consultaPropia() throws Exception {
        getAutenticado("/usuarios/" + sesion.usuarioId(), sesion.token())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sesion.usuarioId()))
                .andExpect(jsonPath("$.clienteId").value(cliente.id()))
                .andExpect(jsonPath("$.correo").value(cliente.correo()))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.fechaCreacion").isNotEmpty())
                .andExpect(jsonPath("$", not(hasKey("password"))));
    }

    @Test
    @DisplayName("No puede consultar el usuario de otra persona: 403; inexistente: 404")
    void propiedadDelUsuario() throws Exception {
        Sesion otra = sesionNueva();
        getAutenticado("/usuarios/" + otra.usuarioId(), sesion.token())
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        getAutenticado("/usuarios/999", sesion.token())
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("USUARIO_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("Cambia su contraseña: 204, la nueva funciona, la anterior no y los tokens anteriores dejan de servir")
    void cambiaContrasena() throws Exception {
        String hashAnterior = jdbc.queryForObject("SELECT password FROM onboarding.usuarios WHERE id = ?", String.class, sesion.usuarioId());

        cambiarPassword(sesion.token(), sesion.usuarioId(), cliente.password(), NUEVA).andExpect(status().isNoContent());

        String hashNuevo = jdbc.queryForObject("SELECT password FROM onboarding.usuarios WHERE id = ?", String.class, sesion.usuarioId());
        assertThat(hashNuevo).startsWith("$2a$").isNotEqualTo(hashAnterior).doesNotContain(NUEVA);
        login(cliente.correo(), NUEVA).andExpect(status().isOk());
        login(cliente.correo(), cliente.password()).andExpect(status().isUnauthorized());
        getAutenticado("/usuarios/" + sesion.usuarioId(), sesion.token())
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("TOKEN_RECHAZADO"));
    }

    @Test
    @DisplayName("Contraseña actual incorrecta: 400 PASSWORD_INVALIDA")
    void actualIncorrecta() throws Exception {
        cambiarPassword(sesion.token(), sesion.usuarioId(), "Incorrecta#2026", NUEVA)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("PASSWORD_INVALIDA"));
    }

    @Test
    @DisplayName("Nueva contraseña débil: 400 con lo que le falta; igual a la actual: 400 PASSWORD_INVALIDA")
    void nuevaInvalida() throws Exception {
        cambiarPassword(sesion.token(), sesion.usuarioId(), cliente.password(), "debil")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("passwordNueva"));
        cambiarPassword(sesion.token(), sesion.usuarioId(), cliente.password(), cliente.password())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("PASSWORD_INVALIDA"));
    }

    @Test
    @DisplayName("No puede cambiar la contraseña de otra persona: 403")
    void contrasenaAjena() throws Exception {
        Sesion otra = sesionNueva();
        cambiarPassword(sesion.token(), otra.usuarioId(), cliente.password(), NUEVA).andExpect(status().isForbidden());
    }
}
