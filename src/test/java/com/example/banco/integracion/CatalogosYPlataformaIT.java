package com.example.banco.integracion;

import com.example.banco.soporte.PruebaIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("API · Catálogos, documentación OpenAPI y comportamiento general")
class CatalogosYPlataformaIT extends PruebaIntegracion {

    @Test
    @DisplayName("Catálogo de estados: 32 entidades con su clave INEGI")
    void estados() throws Exception {
        mvc.perform(get("/catalogos/estados"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=3600, public"))
                .andExpect(jsonPath("$.length()").value(32))
                .andExpect(jsonPath("$[10].id").value(11));
    }

    @Test
    @DisplayName("Catálogo de países ISO 3166-1, sexos, estados civiles y estatus de cuenta")
    void otrosCatalogos() throws Exception {
        mvc.perform(get("/catalogos/paises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(249))
                .andExpect(jsonPath("$[*].codigo", hasItem("MX")));
        mvc.perform(get("/catalogos/sexos")).andExpect(jsonPath("$[*].clave", hasItem("X")));
        mvc.perform(get("/catalogos/estados-civiles")).andExpect(jsonPath("$.length()").value(6));
        mvc.perform(get("/catalogos/estatus-cuenta")).andExpect(jsonPath("$[0].clave").value("ACTIVA"));
    }

    @Test
    @DisplayName("Documentación OpenAPI con todos los endpoints y el esquema de seguridad JWT")
    void openApi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/clientes']").exists())
                .andExpect(jsonPath("$.paths['/clientes/{id}']").exists())
                .andExpect(jsonPath("$.paths['/cuentas/{numeroCuenta}']").exists())
                .andExpect(jsonPath("$.paths['/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/usuarios/{id}/password']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    @Test
    @DisplayName("Health check público para el balanceador")
    void salud() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Cada respuesta lleva X-Request-Id; si el cliente lo envía, se respeta")
    void idDeSolicitud() throws Exception {
        mvc.perform(get("/catalogos/sexos")).andExpect(header().exists("X-Request-Id"));
        mvc.perform(get("/catalogos/sexos").header("X-Request-Id", "prueba-123"))
                .andExpect(header().string("X-Request-Id", "prueba-123"));
        mvc.perform(get("/clientes").header("X-Request-Id", "prueba-456"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.requestId").value("prueba-456"));
    }

    @Test
    @DisplayName("Errores HTTP estándar con el mismo formato: 404 ruta inexistente, 405 método y 415 tipo de contenido")
    void erroresHttp() throws Exception {
        String token = sesionNueva().token();
        mvc.perform(get("/no-existe").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"));
        mvc.perform(patch("/clientes/1").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.codigo").value("METODO_NO_PERMITIDO"));
        mvc.perform(post("/clientes").contentType(MediaType.TEXT_PLAIN).content("hola"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.codigo").value("TIPO_CONTENIDO_NO_SOPORTADO"));
    }

    @Test
    @DisplayName("Los errores usan application/problem+json (RFC 9457)")
    void formatoDeError() throws Exception {
        mvc.perform(get("/clientes"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.startsWith("application/problem+json")))
                .andExpect(jsonPath("$.title").value("No autenticado"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.instance").value("/clientes"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}
