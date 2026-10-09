package com.example.banco.validation;

import com.example.banco.dto.request.CambioPasswordRequest;
import com.example.banco.dto.request.ClienteRequest;
import com.example.banco.soporte.DatosPrueba;
import com.example.banco.util.Constantes;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Validaciones obligatorias de la tarea, probadas directamente con Bean Validation. */
@DisplayName("Validaciones de la solicitud de registro")
class ValidacionSolicitudesTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void crearValidador() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrar() {
        fabrica.close();
    }

    private static Set<String> camposConError(Map<String, Object> datos) {
        ClienteRequest solicitud = DatosPrueba.solicitud(datos);
        return validador.validate(solicitud).stream()
                .map(violacion -> violacion.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static Map<String, Object> clienteCon(String campo, Object valor) {
        Map<String, Object> datos = DatosPrueba.cliente();
        if (campo.startsWith("domicilio.")) {
            DatosPrueba.castMapa(datos.get("domicilio")).put(campo.substring("domicilio.".length()), valor);
        } else {
            datos.put(campo, valor);
        }
        return datos;
    }

    @Test
    @DisplayName("Un cliente con todos sus datos correctos no tiene errores")
    void clienteValido() {
        assertThat(camposConError(DatosPrueba.cliente())).isEmpty();
    }

    @ParameterizedTest(name = "{0} = \"{1}\" es inválido")
    @CsvSource(delimiter = '|', nullValues = "NULO", value = {
            "nombre            | NULO",
            "nombre            | '   '",
            "nombre            | C",
            "nombre            | Carl0s",
            "nombre            | Juan_Carlos",
            "segundoNombre     | A",
            "apellidoPaterno   | NULO",
            "apellidoPaterno   | Gómez3",
            "apellidoMaterno   | M",
            "curp              | NULO",
            "curp              | GOMC900515HQTMNR0",
            "curp              | GOMC900515HXXMNR08",
            "curp              | GOMC901315HQTMNR08",
            "rfc               | NULO",
            "rfc               | GOMC900515A",
            "rfc               | GOMC901315AB7",
            "rfc               | GOMC900515AB77",
            "correo            | NULO",
            "correo            | no-es-correo",
            "correo            | cliente@dominio",
            "correo            | cliente..uno@prueba.mx",
            "telefonoMovil     | NULO",
            "telefonoMovil     | 442123456",
            "telefonoMovil     | 44212345678",
            "telefonoMovil     | 44212345AB",
            "telefonoAlterno   | 12345",
            "nacionalidad      | MEX",
            "nacionalidad      | M",
            "ocupacion         | NULO",
            "empresa           | '  '",
            "password          | NULO",
            "password          | Ab1!xyz",
            "password          | segura#2026",
            "password          | SEGURA#2026",
            "password          | Segura#Clave",
            "password          | Segura2026",
            "domicilio.calle            | NULO",
            "domicilio.numeroExterior   | NULO",
            "domicilio.colonia          | NULO",
            "domicilio.municipio        | NULO",
            "domicilio.codigoPostal     | 7600",
            "domicilio.codigoPostal     | 760000",
            "domicilio.codigoPostal     | 7600A",
            "domicilio.pais             | US"
    })
    @DisplayName("Rechaza cada valor inválido indicando el campo")
    void rechazaValorInvalido(String campo, String valor) {
        assertThat(camposConError(clienteCon(campo, valor))).contains(campo);
    }

    @Test
    @DisplayName("Nombre de 51 caracteres excede el máximo de 50")
    void nombreDemasiadoLargo() {
        assertThat(camposConError(clienteCon("nombre", "a".repeat(51)))).contains("nombre");
        assertThat(camposConError(clienteCon("nombre", "a".repeat(50)))).doesNotContain("nombre");
    }

    @Test
    @DisplayName("Correo de más de 100 caracteres se rechaza")
    void correoDemasiadoLargo() {
        String correo = "a".repeat(95) + "@x.com";
        assertThat(camposConError(clienteCon("correo", correo))).contains("correo");
    }

    @Test
    @DisplayName("Ingreso mensual debe ser mayor a cero y con máximo 2 decimales")
    void ingresoMensual() {
        assertThat(camposConError(clienteCon("ingresoMensual", BigDecimal.ZERO))).contains("ingresoMensual");
        assertThat(camposConError(clienteCon("ingresoMensual", new BigDecimal("-1")))).contains("ingresoMensual");
        assertThat(camposConError(clienteCon("ingresoMensual", new BigDecimal("100.123")))).contains("ingresoMensual");
        assertThat(camposConError(clienteCon("ingresoMensual", new BigDecimal("0.01")))).doesNotContain("ingresoMensual");
    }

    @Test
    @DisplayName("Fecha de nacimiento futura se rechaza")
    void fechaFutura() {
        LocalDate manana = LocalDate.now(Constantes.ZONA_HORARIA).plusDays(1);
        assertThat(camposConError(clienteCon("fechaNacimiento", manana.toString()))).contains("fechaNacimiento");
    }

    @Test
    @DisplayName("Mayoría de edad: 18 años cumplidos hoy se acepta, un día menos se rechaza")
    void mayoriaDeEdad() {
        LocalDate hoy = LocalDate.now(Constantes.ZONA_HORARIA);
        assertThat(camposConError(clienteCon("fechaNacimiento", hoy.minusYears(18).toString())))
                .doesNotContain("fechaNacimiento");
        assertThat(camposConError(clienteCon("fechaNacimiento", hoy.minusYears(18).plusDays(1).toString())))
                .contains("fechaNacimiento");
    }

    @Test
    @DisplayName("Fecha de nacimiento anterior a 1900 se rechaza")
    void fechaMuyAntigua() {
        assertThat(camposConError(clienteCon("fechaNacimiento", "1899-12-31"))).contains("fechaNacimiento");
    }

    @Test
    @DisplayName("Segundo nombre y teléfono alterno son opcionales")
    void opcionales() {
        Map<String, Object> datos = DatosPrueba.cliente();
        datos.put("segundoNombre", null);
        datos.put("telefonoAlterno", "  ");
        DatosPrueba.castMapa(datos.get("domicilio")).put("numeroInterior", null);
        assertThat(camposConError(datos)).isEmpty();
    }

    @Test
    @DisplayName("Normaliza antes de validar: espacios, mayúsculas en CURP/RFC y minúsculas en el correo")
    void normaliza() {
        Map<String, Object> datos = DatosPrueba.cliente();
        datos.put("nombre", "  Carlos   ");
        datos.put("curp", " " + datos.get("curp").toString().toLowerCase() + " ");
        datos.put("rfc", datos.get("rfc").toString().toLowerCase());
        datos.put("correo", "  Cliente.Prueba@PRUEBA.MX ");
        ClienteRequest solicitud = DatosPrueba.solicitud(datos);
        assertThat(validador.validate(solicitud)).isEmpty();
        assertThat(solicitud.nombre()).isEqualTo("Carlos");
        assertThat(solicitud.curp()).isEqualTo(solicitud.curp().toUpperCase());
        assertThat(solicitud.correo()).isEqualTo("cliente.prueba@prueba.mx");
        assertThat(solicitud.domicilio().pais()).isEqualTo("MX");
    }

    @Test
    @DisplayName("El mensaje de la contraseña explica qué le falta")
    void mensajeDeContrasena() {
        Set<ConstraintViolation<ClienteRequest>> errores = validador.validate(DatosPrueba.solicitud(clienteCon("password", "abcdefgh")));
        assertThat(errores).extracting(ConstraintViolation::getMessage)
                .containsExactly("la contraseña requiere: al menos una letra mayúscula, al menos un número, al menos un carácter especial");
    }

    @Test
    @DisplayName("El cambio de contraseña aplica la misma política a la nueva contraseña")
    void cambioDeContrasena() {
        assertThat(validador.validate(new CambioPasswordRequest("Segura#2026", "NuevaClave#2026"))).isEmpty();
        assertThat(validador.validate(new CambioPasswordRequest("Segura#2026", "debil")))
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .containsExactly("passwordNueva");
    }

    @Test
    @DisplayName("toString de las solicitudes nunca muestra la contraseña")
    void noExponeContrasena() {
        assertThat(DatosPrueba.solicitud().toString()).doesNotContain(DatosPrueba.PASSWORD);
        assertThat(new CambioPasswordRequest("Segura#2026", "NuevaClave#2026").toString()).doesNotContain("Segura#2026");
    }
}
