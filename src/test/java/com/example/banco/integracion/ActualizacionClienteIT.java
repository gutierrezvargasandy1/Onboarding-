package com.example.banco.integracion;

import com.example.banco.soporte.DatosPrueba;
import com.example.banco.soporte.PruebaIntegracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("API · Actualización de clientes (PUT /clientes/{id})")
class ActualizacionClienteIT extends PruebaIntegracion {

    private Map<String, Object> datos;
    private ClienteRegistrado cliente;
    private String token;

    @BeforeEach
    void preparar() throws Exception {
        datos = DatosPrueba.cliente();
        cliente = registrarCliente(datos);
        token = iniciarSesion(cliente).token();
    }

    private String url() {
        return "/clientes/" + cliente.id();
    }

    @Test
    @DisplayName("Actualiza datos personales, de contacto, domicilio e información laboral")
    void actualizaTodo() throws Exception {
        Map<String, Object> cambios = DatosPrueba.actualizacion(datos);
        cambios.put("segundoNombre", "Andrés");
        cambios.put("estadoCivil", "SOLTERO");
        cambios.put("telefonoMovil", "4429998877");
        cambios.put("telefonoAlterno", "4421112233");
        cambios.put("ocupacion", "Director de finanzas");
        cambios.put("empresa", "Grupo Financiero del Bajío");
        cambios.put("ingresoMensual", 55000.50);
        Map<String, Object> domicilio = DatosPrueba.castMapa(cambios.get("domicilio"));
        domicilio.put("calle", "Calle Allende");
        domicilio.put("estadoId", 11);
        domicilio.put("codigoPostal", "36000");
        domicilio.put("municipio", "Guanajuato");

        putAutenticado(url(), token, cambios)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoCivil").value("SOLTERO"))
                .andExpect(jsonPath("$.telefonoMovil").value("4429998877"))
                .andExpect(jsonPath("$.telefonoAlterno").value("4421112233"))
                .andExpect(jsonPath("$.ingresoMensual").value(55000.50))
                .andExpect(jsonPath("$.domicilio.estadoId").value(11))
                .andExpect(jsonPath("$.domicilio.codigoPostal").value("36000"))
                .andExpect(jsonPath("$.curp").value(datos.get("curp")));

        Map<String, Object> fila = jdbc.queryForMap(
                "SELECT c.ocupacion, c.version, d.calle, d.estado_id FROM onboarding.clientes c "
                        + "JOIN onboarding.domicilios d ON d.cliente_id = c.id WHERE c.id = ?", cliente.id());
        assertThat(fila.get("ocupacion")).isEqualTo("Director de finanzas");
        assertThat(fila.get("calle")).isEqualTo("Calle Allende");
        assertThat(((Number) fila.get("estado_id")).intValue()).isEqualTo(11);
        assertThat(((Number) fila.get("version")).intValue()).isPositive();
    }

    @Test
    @DisplayName("Cambiar el correo cambia el nombre de usuario: el nuevo correo inicia sesión y el anterior no")
    void cambiaCorreo() throws Exception {
        Map<String, Object> cambios = DatosPrueba.actualizacion(datos);
        cambios.put("correo", "Nuevo.Correo@Prueba.mx");

        putAutenticado(url(), token, cambios).andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("nuevo.correo@prueba.mx"));

        assertThat(jdbc.queryForObject("SELECT correo FROM onboarding.usuarios WHERE cliente_id = ?", String.class, cliente.id()))
                .isEqualTo("nuevo.correo@prueba.mx");
        login("nuevo.correo@prueba.mx", cliente.password()).andExpect(status().isOk());
        login(cliente.correo(), cliente.password()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("No permite modificar la CURP: 400 CAMPO_NO_MODIFICABLE y la CURP no cambia")
    void curpNoModificable() throws Exception {
        Map<String, Object> cambios = DatosPrueba.actualizacion(datos);
        cambios.put("curp", "LOHF950821MGTPRR08");

        putAutenticado(url(), token, cambios)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CAMPO_NO_MODIFICABLE"))
                .andExpect(jsonPath("$.errores[0].campo").value("curp"));
        assertThat(jdbc.queryForObject("SELECT curp FROM onboarding.clientes WHERE id = ?", String.class, cliente.id()))
                .isEqualTo(datos.get("curp"));
    }

    @Test
    @DisplayName("No permite modificar el RFC ni el número de cuenta")
    void rfcYCuentaNoModificables() throws Exception {
        Map<String, Object> conRfc = DatosPrueba.actualizacion(datos);
        conRfc.put("rfc", "LOHF950821QK5");
        putAutenticado(url(), token, conRfc)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores[0].campo").value("rfc"));

        Map<String, Object> conCuenta = DatosPrueba.actualizacion(datos);
        conCuenta.put("numeroCuenta", "1234567897");
        putAutenticado(url(), token, conCuenta)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores[0].campo").value("numeroCuenta"));
    }

    @Test
    @DisplayName("Reenviar la misma CURP, RFC y número de cuenta (p. ej. el JSON de un GET) sí se permite")
    void mismosValoresInmutables() throws Exception {
        Map<String, Object> cambios = DatosPrueba.actualizacion(datos);
        cambios.put("curp", datos.get("curp"));
        cambios.put("rfc", datos.get("rfc"));
        cambios.put("numeroCuenta", cliente.numeroCuenta());
        cambios.put("id", 12345);
        cambios.put("activo", false);

        putAutenticado(url(), token, cambios).andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    @DisplayName("Flujo real: se toma el JSON de GET, se edita y se envía con PUT")
    void getEditarPut() throws Exception {
        String original = contenido(getAutenticado(url(), token).andExpect(status().isOk()).andReturn());
        String editado = original.replace("\"ocupacion\":\"Contador\"", "\"ocupacion\":\"Auditor\"");
        assertThat(editado).isNotEqualTo(original);

        mvc.perform(put(url()).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType("application/json").content(editado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocupacion").value("Auditor"))
                .andExpect(jsonPath("$.cuentas[0].numeroCuenta").value(cliente.numeroCuenta()));
    }

    @Test
    @DisplayName("Correo que ya usa otro cliente: 409 CORREO_DUPLICADO")
    void correoDeOtroCliente() throws Exception {
        ClienteRegistrado otro = registrarCliente();
        Map<String, Object> cambios = DatosPrueba.actualizacion(datos);
        cambios.put("correo", otro.correo());

        putAutenticado(url(), token, cambios)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CORREO_DUPLICADO"));
    }

    @Test
    @DisplayName("Aplica las mismas validaciones que el registro: 400")
    void validaciones() throws Exception {
        Map<String, Object> cambios = DatosPrueba.actualizacion(datos);
        cambios.put("telefonoMovil", "123");
        cambios.put("nombre", "J0sé");
        DatosPrueba.castMapa(cambios.get("domicilio")).put("codigoPostal", "1");

        putAutenticado(url(), token, cambios)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ERROR_VALIDACION"))
                .andExpect(jsonPath("$.errores.length()").value(3))
                .andExpect(jsonPath("$.errores[*].campo",
                        containsInAnyOrder("domicilio.codigoPostal", "nombre", "telefonoMovil")));
    }

    @Test
    @DisplayName("Cambiar solo el domicilio también sube la versión del cliente (bloqueo optimista de todo el registro)")
    void soloDomicilioIncrementaVersion() throws Exception {
        String consulta = "SELECT version FROM onboarding.clientes WHERE id = ?";
        Integer antes = jdbc.queryForObject(consulta, Integer.class, cliente.id());
        Map<String, Object> cambios = DatosPrueba.actualizacion(datos);
        DatosPrueba.castMapa(cambios.get("domicilio")).put("calle", "Calle Nueva");

        putAutenticado(url(), token, cambios)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.domicilio.calle").value("Calle Nueva"));

        assertThat(jdbc.queryForObject(consulta, Integer.class, cliente.id())).isGreaterThan(antes);
        assertThat(jdbc.queryForObject("SELECT calle FROM onboarding.domicilios WHERE cliente_id = ?",
                String.class, cliente.id())).isEqualTo("Calle Nueva");
    }

    @Test
    @DisplayName("Cambiar la fecha de nacimiento a una distinta de la CURP: 400")
    void fechaIncoherente() throws Exception {
        Map<String, Object> cambios = DatosPrueba.actualizacion(datos);
        cambios.put("fechaNacimiento", "1991-05-15");
        putAutenticado(url(), token, cambios).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cliente inexistente 404; cliente dado de baja 409 CLIENTE_INACTIVO")
    void inexistenteOInactivo() throws Exception {
        Map<String, Object> cambios = DatosPrueba.actualizacion(datos);
        putAutenticado("/clientes/999", token, cambios).andExpect(status().isNotFound());

        ClienteRegistrado otro = registrarCliente();
        mvc.perform(delete("/clientes/" + otro.id()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
        putAutenticado("/clientes/" + otro.id(), token, cambios)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CLIENTE_INACTIVO"));
    }

    @Test
    @DisplayName("Sin token: 401")
    void sinToken() throws Exception {
        mvc.perform(put(url()).contentType("application/json").content(json(DatosPrueba.actualizacion(datos))))
                .andExpect(status().isUnauthorized());
    }
}
