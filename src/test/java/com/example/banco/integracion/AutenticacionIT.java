package com.example.banco.integracion;

import com.example.banco.soporte.DatosPrueba;
import com.example.banco.soporte.PruebaIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("API · Inicio de sesión y protección con JWT")
class AutenticacionIT extends PruebaIntegracion {

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    @DisplayName("Credenciales correctas: 200 con token Bearer, vigencia, usuarioId y clienteId")
    void loginCorrecto() throws Exception {
        ClienteRegistrado cliente = registrarCliente();

        MvcResult resultado = login(cliente.correo(), cliente.password())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEnSegundos").value(3600))
                .andExpect(jsonPath("$.usuarioId").value(1))
                .andExpect(jsonPath("$.clienteId").value(cliente.id()))
                .andExpect(jsonPath("$.correo").value(cliente.correo()))
                .andReturn();

        String token = leer(resultado, "$.token");
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("El token da acceso a los endpoints protegidos")
    void tokenDaAcceso() throws Exception {
        ClienteRegistrado cliente = registrarCliente();
        Sesion sesion = iniciarSesion(cliente);

        getAutenticado("/clientes/" + cliente.id(), sesion.token())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cliente.id()));
    }

    @Test
    @DisplayName("El correo del login no distingue mayúsculas ni espacios")
    void correoNormalizado() throws Exception {
        ClienteRegistrado cliente = registrarCliente();
        login("  " + cliente.correo().toUpperCase() + " ", cliente.password()).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Contraseña incorrecta: 401 CREDENCIALES_INVALIDAS")
    void contrasenaIncorrecta() throws Exception {
        ClienteRegistrado cliente = registrarCliente();
        login(cliente.correo(), "Incorrecta#2026")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    @DisplayName("Correo no registrado: 401 con el mismo mensaje (no revela qué correos existen)")
    void correoInexistente() throws Exception {
        ClienteRegistrado cliente = registrarCliente();
        MvcResult inexistente = login("nadie-" + UUID.randomUUID() + "@prueba.mx", DatosPrueba.PASSWORD)
                .andExpect(status().isUnauthorized()).andReturn();
        MvcResult incorrecta = login(cliente.correo(), "Incorrecta#2026")
                .andExpect(status().isUnauthorized()).andReturn();

        assertThat((String) leer(inexistente, "$.detail")).isEqualTo(leer(incorrecta, "$.detail"));
        assertThat((String) leer(inexistente, "$.codigo")).isEqualTo("CREDENCIALES_INVALIDAS");
    }

    @Test
    @DisplayName("Usuario inactivo (cliente dado de baja): 403 USUARIO_INACTIVO")
    void usuarioInactivo() throws Exception {
        ClienteRegistrado cliente = registrarCliente();
        Sesion sesion = iniciarSesion(cliente);
        mvc.perform(delete("/clientes/" + cliente.id()).header(HttpHeaders.AUTHORIZATION, bearer(sesion.token())))
                .andExpect(status().isNoContent());

        login(cliente.correo(), cliente.password())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("USUARIO_INACTIVO"));
    }

    @Test
    @DisplayName("Login sin datos: 400 con los campos obligatorios")
    void loginIncompleto() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.length()").value(2));
    }

    @Test
    @DisplayName("Tras 3 intentos fallidos el correo se bloquea: 429 con Retry-After, incluso con la contraseña correcta")
    void bloqueoPorIntentos() throws Exception {
        ClienteRegistrado cliente = registrarCliente();
        for (int i = 0; i < 3; i++) {
            login(cliente.correo(), "Incorrecta#2026").andExpect(status().isUnauthorized());
        }

        login(cliente.correo(), cliente.password())
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.codigo").value("DEMASIADOS_INTENTOS"));
    }

    @Test
    @DisplayName("Sin token: 401 NO_AUTENTICADO con WWW-Authenticate: Bearer")
    void sinToken() throws Exception {
        mvc.perform(get("/clientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Token alterado o basura: 401 TOKEN_INVALIDO")
    void tokenInvalido() throws Exception {
        Sesion sesion = sesionNueva();
        String alterado = sesion.token().substring(0, sesion.token().length() - 4) + "abcd";

        getAutenticado("/clientes", alterado).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
        getAutenticado("/clientes", "esto.no.es-un-jwt").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token vencido: 401")
    void tokenVencido() throws Exception {
        registrarCliente();
        Instant haceDosHoras = Instant.now().minusSeconds(7200);
        String vencido = firmar(JwtClaimsSet.builder().issuer("banco-onboarding").audience(List.of("banco-onboarding-api"))
                .subject("1").issuedAt(haceDosHoras).expiresAt(haceDosHoras.plusSeconds(3600)).claim("ver", 0).build());

        getAutenticado("/clientes", vencido).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
    }

    @Test
    @DisplayName("Token de otro emisor o para otra audiencia: 401")
    void emisorOAudienciaIncorrectos() throws Exception {
        registrarCliente();
        Instant ahora = Instant.now();
        String otroEmisor = firmar(JwtClaimsSet.builder().issuer("otro-sistema").audience(List.of("banco-onboarding-api"))
                .subject("1").issuedAt(ahora).expiresAt(ahora.plusSeconds(600)).claim("ver", 0).build());
        String otraAudiencia = firmar(JwtClaimsSet.builder().issuer("banco-onboarding").audience(List.of("otra-api"))
                .subject("1").issuedAt(ahora).expiresAt(ahora.plusSeconds(600)).claim("ver", 0).build());

        getAutenticado("/clientes", otroEmisor).andExpect(status().isUnauthorized());
        getAutenticado("/clientes", otraAudiencia).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Endpoints públicos sin token: registro, login, catálogos, documentación y salud")
    void endpointsPublicos() throws Exception {
        mvc.perform(get("/catalogos/estados")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        login("nadie-" + UUID.randomUUID() + "@prueba.mx", DatosPrueba.PASSWORD).andExpect(status().isUnauthorized());
        registrar(DatosPrueba.cliente()).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Endpoints protegidos rechazan peticiones sin token")
    void endpointsProtegidos() throws Exception {
        for (String url : List.of("/clientes", "/clientes/1", "/clientes/activos", "/clientes/curp/GOMC900515HQTMNR08",
                "/cuentas/activas", "/cuentas/1000000016", "/cuentas/1000000016/saldo", "/usuarios/1")) {
            mvc.perform(get(url)).andExpect(status().isUnauthorized());
        }
        mvc.perform(delete("/clientes/1")).andExpect(status().isUnauthorized());
    }

    private String firmar(JwtClaimsSet claims) {
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

}
