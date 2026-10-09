package com.example.banco.soporte;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de las pruebas de integración: aplicación completa (seguridad, validación,
 * JPA, Flyway, triggers) contra PostgreSQL real. Cada prueba inicia con las tablas vacías.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class PruebaIntegracion {

    protected static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @DynamicPropertySource
    static void baseDeDatos(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> PostgresEmbebido.jdbcUrl("postgres"));
        registro.add("spring.datasource.username", () -> "postgres");
        registro.add("spring.datasource.password", () -> "postgres");
    }

    @BeforeEach
    void vaciarTablas() {
        jdbc.execute("TRUNCATE onboarding.usuarios, onboarding.cuentas, onboarding.domicilios, onboarding.clientes RESTART IDENTITY");
    }

    // ------------------------------------------------------------- utilerías

    protected static String json(Object valor) {
        return JSON.writeValueAsString(valor);
    }

    protected static String contenido(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    protected static <T> T leer(MvcResult resultado, String rutaJson) throws Exception {
        return JsonPath.read(contenido(resultado), rutaJson);
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected ResultActions registrar(Map<String, Object> cliente) throws Exception {
        return mvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(json(cliente)));
    }

    protected ClienteRegistrado registrarCliente(Map<String, Object> cliente) throws Exception {
        MvcResult resultado = registrar(cliente).andExpect(status().isCreated()).andReturn();
        Integer id = leer(resultado, "$.id");
        String numeroCuenta = leer(resultado, "$.cuentas[0].numeroCuenta");
        return new ClienteRegistrado(id, numeroCuenta, (String) cliente.get("correo"), (String) cliente.get("password"));
    }

    protected ClienteRegistrado registrarCliente() throws Exception {
        return registrarCliente(DatosPrueba.cliente());
    }

    protected ResultActions login(String correo, String password) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("correo", correo, "password", password))));
    }

    protected Sesion iniciarSesion(ClienteRegistrado cliente) throws Exception {
        MvcResult resultado = login(cliente.correo(), cliente.password()).andExpect(status().isOk()).andReturn();
        Integer usuarioId = leer(resultado, "$.usuarioId");
        String token = leer(resultado, "$.token");
        return new Sesion(token, usuarioId);
    }

    /** Cliente registrado y con sesión iniciada: atajo para las pruebas de endpoints protegidos. */
    protected Sesion sesionNueva() throws Exception {
        return iniciarSesion(registrarCliente());
    }

    protected ResultActions getAutenticado(String url, String token) throws Exception {
        return mvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    protected ResultActions putAutenticado(String url, String token, Object cuerpo) throws Exception {
        return mvc.perform(put(url).header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json(cuerpo)));
    }

    protected int contarFilas(String tabla) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM onboarding." + tabla, Integer.class);
        return total == null ? 0 : total;
    }

    public record ClienteRegistrado(Integer id, String numeroCuenta, String correo, String password) {
    }

    public record Sesion(String token, Integer usuarioId) {
    }
}
