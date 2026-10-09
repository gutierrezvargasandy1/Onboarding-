package com.example.banco.integracion;

import com.example.banco.soporte.DatosPrueba;
import com.example.banco.soporte.PruebaIntegracion;
import com.example.banco.util.Constantes;
import com.example.banco.util.NumeroCuenta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("API · Registro de clientes (POST /clientes)")
class RegistroClienteIT extends PruebaIntegracion {

    @Test
    @DisplayName("Registra al cliente: 201, Location, cuenta ACTIVA con número único y saldo inicial; nunca devuelve la contraseña")
    void registraCliente() throws Exception {
        Map<String, Object> cliente = DatosPrueba.cliente();

        MvcResult resultado = registrar(cliente)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/clientes/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.curp").value(cliente.get("curp")))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.fechaRegistro").isNotEmpty())
                .andExpect(jsonPath("$.domicilio.estadoId").value(22))
                .andExpect(jsonPath("$.cuentas.length()").value(1))
                .andExpect(jsonPath("$.cuentas[0].estatus").value("ACTIVA"))
                .andExpect(jsonPath("$.cuentas[0].saldo").value(1000.00))
                .andExpect(jsonPath("$.cuentas[0].moneda").value("MXN"))
                .andExpect(jsonPath("$.cuentas[0].clienteId").value(1))
                .andExpect(jsonPath("$", not(hasKey("password"))))
                .andReturn();

        String numeroCuenta = leer(resultado, "$.cuentas[0].numeroCuenta");
        assertThat(numeroCuenta).matches("\\d{10}");
        assertThat(NumeroCuenta.esValido(numeroCuenta)).isTrue();
        assertThat(contenido(resultado)).doesNotContain(DatosPrueba.PASSWORD);
        assertThat((String) leer(resultado, "$.domicilio.estado")).isEqualTo("Querétaro");
        assertThat((String) leer(resultado, "$.apellidoPaterno")).isEqualTo("Gómez");
    }

    @Test
    @DisplayName("Crea el usuario de acceso: correo como nombre de usuario, contraseña cifrada con BCrypt y activo")
    void creaUsuarioDeAcceso() throws Exception {
        Map<String, Object> cliente = DatosPrueba.cliente();
        registrarCliente(cliente);

        Map<String, Object> usuario = jdbc.queryForMap(
                "SELECT cliente_id, correo, password, activo, fecha_creacion, fecha_actualizacion FROM onboarding.usuarios");
        assertThat(usuario.get("correo")).isEqualTo(cliente.get("correo"));
        assertThat((String) usuario.get("password")).startsWith("$2a$").isNotEqualTo(DatosPrueba.PASSWORD).hasSize(60);
        assertThat(usuario.get("activo")).isEqualTo(true);
        assertThat(usuario.get("fecha_creacion")).isNotNull();
        assertThat(contarFilas("usuarios")).isEqualTo(1);
    }

    @Test
    @DisplayName("Normaliza los datos: CURP y RFC en mayúsculas, correo en minúsculas y sin espacios sobrantes")
    void normaliza() throws Exception {
        Map<String, Object> cliente = DatosPrueba.cliente();
        String curp = (String) cliente.get("curp");
        cliente.put("curp", " " + curp.toLowerCase() + " ");
        cliente.put("rfc", ((String) cliente.get("rfc")).toLowerCase());
        cliente.put("correo", "  Carlos.Gomez@PRUEBA.mx ");
        cliente.put("nombre", "  Carlos  ");

        registrar(cliente)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.curp").value(curp))
                .andExpect(jsonPath("$.correo").value("carlos.gomez@prueba.mx"))
                .andExpect(jsonPath("$.nombre").value("Carlos"));
    }

    @Test
    @DisplayName("Misma CURP y mismo RFC: 409 CLIENTE_YA_REGISTRADO")
    void clienteYaRegistrado() throws Exception {
        Map<String, Object> cliente = DatosPrueba.cliente();
        registrarCliente(cliente);
        Map<String, Object> repetido = new LinkedHashMap<>(cliente);
        repetido.put("correo", "otro.correo@prueba.mx");

        registrar(repetido)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CLIENTE_YA_REGISTRADO"))
                .andExpect(jsonPath("$.status").value(409));
        assertThat(contarFilas("clientes")).isEqualTo(1);
    }

    @Test
    @DisplayName("CURP de otro cliente: 409 CURP_DUPLICADA")
    void curpDuplicada() throws Exception {
        Map<String, Object> primero = DatosPrueba.cliente();
        registrarCliente(primero);
        Map<String, Object> segundo = DatosPrueba.cliente();
        segundo.put("curp", primero.get("curp"));

        registrar(segundo).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CURP_DUPLICADA"));
    }

    @Test
    @DisplayName("RFC de otro cliente: 409 RFC_DUPLICADO")
    void rfcDuplicado() throws Exception {
        Map<String, Object> primero = DatosPrueba.cliente();
        registrarCliente(primero);
        Map<String, Object> segundo = DatosPrueba.cliente();
        segundo.put("rfc", primero.get("rfc"));

        registrar(segundo).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("RFC_DUPLICADO"));
    }

    @Test
    @DisplayName("Correo de otro cliente (aunque cambien mayúsculas): 409 CORREO_DUPLICADO")
    void correoDuplicado() throws Exception {
        Map<String, Object> primero = DatosPrueba.cliente();
        registrarCliente(primero);
        Map<String, Object> segundo = DatosPrueba.cliente();
        segundo.put("correo", ((String) primero.get("correo")).toUpperCase());

        registrar(segundo).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CORREO_DUPLICADO"));
    }

    @ParameterizedTest(name = "{0} = \"{1}\" -> 400")
    @CsvSource(delimiter = '|', nullValues = "NULO", value = {
            "nombre                  | NULO",
            "nombre                  | Carl0s",
            "nombre                  | C",
            "apellidoMaterno         | M3ndoza",
            "curp                    | GOMC900515HQTMNR0",
            "curp                    | GOMC900515HXXMNR08",
            "rfc                     | GOMC900515A",
            "rfc                     | GOMC901315AB7",
            "correo                  | no-es-un-correo",
            "telefonoMovil           | 442123456",
            "telefonoMovil           | 44212345678",
            "telefonoAlterno         | 4421",
            "nacionalidad            | MEX",
            "password                | debil",
            "password                | SinNumeros#",
            "password                | sinmayuscula1#",
            "password                | SINMINUSCULA1#",
            "password                | SinEspecial123",
            "domicilio.codigoPostal  | 7600",
            "domicilio.codigoPostal  | 7600A",
            "domicilio.estadoId      | 33",
            "domicilio.pais          | US",
            "domicilio.calle         | NULO"
    })
    @DisplayName("Rechaza datos inválidos con 400 ERROR_VALIDACION e indica el campo")
    void rechazaDatosInvalidos(String campo, String valor) throws Exception {
        Map<String, Object> cliente = DatosPrueba.cliente();
        if (campo.startsWith("domicilio.")) {
            Object convertido = campo.endsWith("estadoId") ? (Object) Integer.valueOf(valor) : valor;
            DatosPrueba.castMapa(cliente.get("domicilio")).put(campo.substring("domicilio.".length()), convertido);
        } else {
            cliente.put(campo, valor);
        }

        registrar(cliente)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ERROR_VALIDACION"))
                .andExpect(jsonPath("$.errores[*].campo", hasItem(campo)));
        assertThat(contarFilas("clientes")).isZero();
    }

    @Test
    @DisplayName("Ingreso mensual en cero o negativo: 400")
    void ingresoMensual() throws Exception {
        for (Object ingreso : List.of(0, -5000)) {
            Map<String, Object> cliente = DatosPrueba.cliente();
            cliente.put("ingresoMensual", ingreso);
            registrar(cliente).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores[*].campo", hasItem("ingresoMensual")));
        }
    }

    @Test
    @DisplayName("Fecha de nacimiento futura: 400")
    void fechaFutura() throws Exception {
        LocalDate manana = LocalDate.now(Constantes.ZONA_HORARIA).plusDays(1);
        registrar(DatosPrueba.cliente(manana))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("fechaNacimiento")))
                .andExpect(jsonPath("$.errores[*].mensaje", hasItem("no puede ser una fecha futura")));
    }

    @Test
    @DisplayName("Menor de edad (cumple 18 años mañana): 400; con 18 años cumplidos hoy: 201")
    void mayoriaDeEdad() throws Exception {
        LocalDate hoy = LocalDate.now(Constantes.ZONA_HORARIA);
        registrar(DatosPrueba.cliente(hoy.minusYears(18).plusDays(1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].mensaje", hasItem("el cliente debe ser mayor de edad (18 años o más)")));
        registrar(DatosPrueba.cliente(hoy.minusYears(18))).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("La fecha de nacimiento debe coincidir con la de la CURP: 400")
    void coherenciaCurp() throws Exception {
        Map<String, Object> cliente = DatosPrueba.cliente();
        cliente.put("fechaNacimiento", "1990-05-16");
        registrar(cliente)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("curp")));
    }

    @Test
    @DisplayName("Nacionalidad que no existe en el catálogo ISO: 400")
    void nacionalidadInexistente() throws Exception {
        Map<String, Object> cliente = DatosPrueba.cliente();
        cliente.put("nacionalidad", "ZZ");
        registrar(cliente).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores[0].campo").value("nacionalidad"));
    }

    @Test
    @DisplayName("Valor de catálogo inválido (sexo = Z): 400 con los valores permitidos")
    void catalogoInvalido() throws Exception {
        Map<String, Object> cliente = DatosPrueba.cliente();
        cliente.put("sexo", "Z");
        registrar(cliente)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("sexo"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("valores permitidos: H, M, X"));
    }

    @Test
    @DisplayName("Fecha con formato incorrecto: 400")
    void fechaMalFormada() throws Exception {
        Map<String, Object> cliente = DatosPrueba.cliente();
        cliente.put("fechaNacimiento", "15/05/1990");
        registrar(cliente).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("ERROR_VALIDACION"));
    }

    @Test
    @DisplayName("JSON mal formado: 400 JSON_INVALIDO; cuerpo vacío: 400 con todos los campos obligatorios")
    void jsonInvalido() throws Exception {
        mvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("JSON_INVALIDO"));

        mvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("nombre")))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("curp")))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("rfc")))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("domicilio")))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("password")));
    }

    @Test
    @DisplayName("Segundo nombre, teléfono alterno y número interior son opcionales")
    void camposOpcionales() throws Exception {
        Map<String, Object> cliente = DatosPrueba.cliente();
        cliente.remove("segundoNombre");
        cliente.remove("telefonoAlterno");
        DatosPrueba.castMapa(cliente.get("domicilio")).remove("numeroInterior");
        DatosPrueba.castMapa(cliente.get("domicilio")).remove("pais");

        registrar(cliente)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.segundoNombre").doesNotExist())
                .andExpect(jsonPath("$.domicilio.pais").value("MX"));
    }

    @Test
    @DisplayName("Registro público: no requiere token")
    void registroPublico() throws Exception {
        registrar(DatosPrueba.cliente()).andExpect(status().isCreated());
    }
}
