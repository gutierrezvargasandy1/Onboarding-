package com.example.banco.exception;

import com.example.banco.dto.response.RespuestaError;
import com.example.banco.entity.Cliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler: traducción de excepciones a respuestas de error")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler manejador = new GlobalExceptionHandler();
    private final MockHttpServletRequest peticion = new MockHttpServletRequest("PUT", "/clientes/1");

    /** Error tal como lo envía PostgreSQL: campos separados por \0 ('C' = SQLSTATE, 'n' = restricción). */
    private static DataIntegrityViolationException violacion(String sqlState, String restriccion) {
        ServerErrorMessage mensaje = new ServerErrorMessage(
                "SERROR\0C" + sqlState + "\0Mviolación de prueba\0n" + restriccion + "\0");
        return new DataIntegrityViolationException("violación", new PSQLException(mensaje));
    }

    @Test
    @DisplayName("Modificación simultánea (bloqueo optimista): 409 CONFLICTO_CONCURRENCIA")
    void concurrencia() {
        ResponseEntity<RespuestaError> respuesta = manejador.concurrencia(
                new ObjectOptimisticLockingFailureException(Cliente.class, 1), peticion);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(409);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().codigo()).isEqualTo("CONFLICTO_CONCURRENCIA");
        assertThat(respuesta.getBody().instance()).isEqualTo("/clientes/1");
        assertThat(respuesta.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @ParameterizedTest(name = "{1} → {2} {3}")
    @CsvSource({
            "23505, uq_clientes_curp,            409, CURP_DUPLICADA",
            "23505, uq_clientes_rfc,             409, RFC_DUPLICADO",
            "23505, uq_usuarios_correo,          409, CORREO_DUPLICADO",
            "23514, tg_clientes_mayor_edad,      400, ERROR_VALIDACION",
            "23000, tg_clientes_curp_inmutable,  400, CAMPO_NO_MODIFICABLE",
            "23514, tg_cuentas_cliente_inactivo, 409, CLIENTE_INACTIVO",
            "23001, tg_baja_logica,              409, BAJA_LOGICA",
            "23514, ck_clientes_ingreso,         400, ERROR_VALIDACION",
            "23503, fk_desconocida,              409, INTEGRIDAD_DATOS"
    })
    @DisplayName("Una restricción o trigger de PostgreSQL se traduce a un error claro por su nombre")
    void restriccionesDeLaBase(String sqlState, String restriccion, int estatus, String codigo) {
        ResponseEntity<RespuestaError> respuesta = manejador.integridad(violacion(sqlState, restriccion), peticion);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(estatus);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().codigo()).isEqualTo(codigo);
        assertThat(respuesta.getBody().detail()).doesNotContain(restriccion);
    }

    @Test
    @DisplayName("Error inesperado: 500 ERROR_INTERNO sin exponer el mensaje interno")
    void errorInterno() {
        ResponseEntity<RespuestaError> respuesta = manejador.general(
                new IllegalStateException("SELECT * FROM onboarding.usuarios"), peticion);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(500);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().codigo()).isEqualTo("ERROR_INTERNO");
        assertThat(respuesta.getBody().detail()).doesNotContain("SELECT").doesNotContain("usuarios");
    }
}
