package com.example.banco.integracion;

import com.example.banco.soporte.DatosPrueba;
import com.example.banco.soporte.PruebaIntegracion;
import com.example.banco.util.Constantes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("API · Consultas de clientes")
class ConsultaClientesIT extends PruebaIntegracion {

    private Map<String, Object> primero;
    private ClienteRegistrado clientePrimero;
    private ClienteRegistrado clienteSegundo;
    private ClienteRegistrado clienteTercero;
    private String token;

    @BeforeEach
    void registrarTresClientes() throws Exception {
        primero = DatosPrueba.cliente();
        clientePrimero = registrarCliente(primero);
        clienteSegundo = registrarCliente();
        clienteTercero = registrarCliente();
        token = iniciarSesion(clientePrimero).token();
    }

    @Test
    @DisplayName("Consulta todos los clientes paginados y ordenados por id")
    void todos() throws Exception {
        getAutenticado("/clientes", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.contenido.length()").value(3))
                .andExpect(jsonPath("$.contenido[0].id").value(clientePrimero.id()))
                .andExpect(jsonPath("$.contenido[2].id").value(clienteTercero.id()))
                .andExpect(jsonPath("$.contenido[0].cuentas[0].estatus").value("ACTIVA"));
    }

    @Test
    @DisplayName("Paginación: page y size")
    void paginacion() throws Exception {
        getAutenticado("/clientes?page=1&size=2", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamano").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.ultima").value(true));
        getAutenticado("/clientes?sort=id,desc", token)
                .andExpect(jsonPath("$.contenido[0].id").value(clienteTercero.id()));
    }

    @Test
    @DisplayName("Solo se puede ordenar por campos permitidos: 400")
    void ordenamientoNoPermitido() throws Exception {
        getAutenticado("/clientes?sort=password", token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("sort"));
    }

    @Test
    @DisplayName("Consulta por ID con domicilio y cuentas; inexistente 404; id inválido 400")
    void porId() throws Exception {
        getAutenticado("/clientes/" + clienteSegundo.id(), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clienteSegundo.id()))
                .andExpect(jsonPath("$.domicilio.codigoPostal").value("76000"))
                .andExpect(jsonPath("$.cuentas[0].numeroCuenta").value(clienteSegundo.numeroCuenta()));
        getAutenticado("/clientes/999", token)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CLIENTE_NO_ENCONTRADO"));
        getAutenticado("/clientes/0", token).andExpect(status().isBadRequest());
        getAutenticado("/clientes/abc", token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
    }

    @Test
    @DisplayName("Busca por CURP (también en minúsculas); inexistente 404; formato inválido 400")
    void porCurp() throws Exception {
        String curp = (String) primero.get("curp");
        getAutenticado("/clientes/curp/" + curp, token)
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(clientePrimero.id()));
        getAutenticado("/clientes/curp/" + curp.toLowerCase(), token)
                .andExpect(status().isOk()).andExpect(jsonPath("$.curp").value(curp));
        getAutenticado("/clientes/curp/LOHF950821MGTPRR08", token)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("CLIENTE_NO_ENCONTRADO"));
        getAutenticado("/clientes/curp/NO-ES-CURP", token)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores[0].campo").value("curp"));
    }

    @Test
    @DisplayName("Busca por RFC; inexistente 404")
    void porRfc() throws Exception {
        getAutenticado("/clientes/rfc/" + primero.get("rfc"), token)
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(clientePrimero.id()));
        getAutenticado("/clientes/rfc/LOHF950821QK5", token).andExpect(status().isNotFound());
        getAutenticado("/clientes/rfc/ABC", token).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Busca por correo; inexistente 404")
    void porCorreo() throws Exception {
        getAutenticado("/clientes/correo/" + clienteSegundo.correo(), token)
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(clienteSegundo.id()));
        getAutenticado("/clientes/correo/nadie@prueba.mx", token).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Consulta por número de cuenta; inexistente 404; formato inválido 400")
    void porNumeroCuenta() throws Exception {
        getAutenticado("/clientes/cuenta/" + clienteTercero.numeroCuenta(), token)
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(clienteTercero.id()));
        getAutenticado("/clientes/cuenta/1234567897", token).andExpect(status().isNotFound());
        getAutenticado("/clientes/cuenta/12345", token).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Clientes activos: excluye a los dados de baja")
    void activos() throws Exception {
        mvc.perform(delete("/clientes/" + clienteSegundo.id()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        getAutenticado("/clientes/activos", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.contenido[0].id").value(clientePrimero.id()))
                .andExpect(jsonPath("$.contenido[1].id").value(clienteTercero.id()));
        getAutenticado("/clientes", token).andExpect(jsonPath("$.totalElementos").value(3));
    }

    @Test
    @DisplayName("Clientes registrados en un rango de fechas (ambas incluidas)")
    void rangoDeFechas() throws Exception {
        LocalDate hoy = LocalDate.now(Constantes.ZONA_HORARIA);
        getAutenticado("/clientes/registrados?desde=" + hoy + "&hasta=" + hoy, token)
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(3));
        getAutenticado("/clientes/registrados?desde=" + hoy.minusDays(30) + "&hasta=" + hoy.minusDays(1), token)
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    @DisplayName("Rango de fechas inválido: invertido, incompleto o con formato incorrecto (400)")
    void rangoInvalido() throws Exception {
        getAutenticado("/clientes/registrados?desde=2026-10-06&hasta=2026-10-01", token)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores[0].campo").value("desde"));
        getAutenticado("/clientes/registrados?desde=2026-10-01", token)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("PARAMETRO_FALTANTE"));
        getAutenticado("/clientes/registrados?desde=01/10/2026&hasta=2026-10-06", token)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
    }
}
