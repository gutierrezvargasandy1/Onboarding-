package com.example.banco.exception;

import com.example.banco.dto.response.RespuestaError;
import com.example.banco.security.TokenRechazadoException;
import com.example.banco.web.FiltroIdSolicitud;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Map.entry;

/**
 * Traduce cualquier excepción a una respuesta JSON uniforme ({@link RespuestaError},
 * basada en RFC 9457) con el estatus HTTP correcto. Nunca expone trazas, SQL ni
 * detalles internos: los errores inesperados se registran en el log con su requestId.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_VALIDACION = "ERROR_VALIDACION";
    private static final Pattern CAMPO_JSON = Pattern.compile("\\[\"([^\"]+)\"]");

    /** Restricciones y triggers de PostgreSQL (por nombre) que pueden saltar si dos peticiones compiten. */
    private static final Map<String, ReglaBd> RESTRICCIONES_BD = Map.ofEntries(
            entry("uq_clientes_curp", new ReglaBd(HttpStatus.CONFLICT, "CURP_DUPLICADA",
                    "Ya existe un cliente registrado con esa CURP")),
            entry("uq_clientes_rfc", new ReglaBd(HttpStatus.CONFLICT, "RFC_DUPLICADO",
                    "Ya existe un cliente registrado con ese RFC")),
            entry("uq_clientes_correo", new ReglaBd(HttpStatus.CONFLICT, "CORREO_DUPLICADO",
                    "Ya existe un cliente o usuario registrado con ese correo electrónico")),
            entry("uq_usuarios_correo", new ReglaBd(HttpStatus.CONFLICT, "CORREO_DUPLICADO",
                    "Ya existe un cliente o usuario registrado con ese correo electrónico")),
            entry("uq_usuarios_cliente", new ReglaBd(HttpStatus.CONFLICT, "USUARIO_DUPLICADO",
                    "El cliente ya tiene un usuario de acceso")),
            entry("pk_domicilios", new ReglaBd(HttpStatus.CONFLICT, "DOMICILIO_DUPLICADO",
                    "El cliente ya tiene un domicilio registrado")),
            entry("uq_cuentas_numero", new ReglaBd(HttpStatus.CONFLICT, "NUMERO_CUENTA_DUPLICADO",
                    "El número de cuenta ya existe")),
            entry("tg_clientes_curp_inmutable", new ReglaBd(HttpStatus.BAD_REQUEST, "CAMPO_NO_MODIFICABLE",
                    "La CURP no puede modificarse")),
            entry("tg_clientes_rfc_inmutable", new ReglaBd(HttpStatus.BAD_REQUEST, "CAMPO_NO_MODIFICABLE",
                    "El RFC no puede modificarse")),
            entry("tg_cuentas_numero_inmutable", new ReglaBd(HttpStatus.BAD_REQUEST, "CAMPO_NO_MODIFICABLE",
                    "El número de cuenta no puede modificarse")),
            entry("tg_clientes_mayor_edad", new ReglaBd(HttpStatus.BAD_REQUEST, ERROR_VALIDACION,
                    "El cliente debe ser mayor de edad (18 años o más)")),
            entry("tg_clientes_fecha_futura", new ReglaBd(HttpStatus.BAD_REQUEST, ERROR_VALIDACION,
                    "La fecha de nacimiento no puede ser futura")),
            entry("tg_cuentas_cliente_inactivo", new ReglaBd(HttpStatus.CONFLICT, "CLIENTE_INACTIVO",
                    "Solo los clientes activos pueden tener cuentas activas")),
            entry("tg_usuarios_cliente_inactivo", new ReglaBd(HttpStatus.CONFLICT, "CLIENTE_INACTIVO",
                    "Un cliente dado de baja no puede tener un usuario activo")),
            entry("tg_baja_logica", new ReglaBd(HttpStatus.CONFLICT, "BAJA_LOGICA",
                    "No se permite eliminar registros: la baja es lógica")),
            entry("ck_cuentas_saldo", new ReglaBd(HttpStatus.BAD_REQUEST, ERROR_VALIDACION,
                    "El saldo no puede ser negativo")));

    // ------------------------------------------------------------------ negocio

    @ExceptionHandler(NegocioException.class)
    ResponseEntity<RespuestaError> negocio(NegocioException ex, HttpServletRequest request) {
        HttpHeaders encabezados = new HttpHeaders();
        if (ex instanceof DemasiadosIntentosException demasiados) {
            encabezados.set(HttpHeaders.RETRY_AFTER, String.valueOf(demasiados.getSegundosParaReintentar()));
        }
        log.debug("Regla de negocio: {} - {}", ex.getCodigo(), ex.getMessage());
        return respuesta(ex.getEstatus(), ex.getCodigo(), ex.getMessage(), request, ex.getErrores(), encabezados);
    }

    // --------------------------------------------------------------- validación

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<RespuestaError> cuerpoInvalido(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorCampo> errores = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.add(new ErrorCampo(error.getField(), error.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> errores.add(new ErrorCampo(error.getObjectName(), error.getDefaultMessage())));
        return errorValidacion(errores, request);
    }

    /**
     * Validación de métodos: ocurre cuando el método del controlador tiene una restricción
     * directa en un parámetro (p. ej. {@code @Positive Integer id}). En ese caso Spring valida
     * también el {@code @Valid @RequestBody} por esta vía, así que los errores del cuerpo se
     * reportan con el nombre de su campo (p. ej. "telefonoMovil") y los de la ruta o de la
     * consulta con el nombre del parámetro (p. ej. "id").
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<RespuestaError> parametrosInvalidos(HandlerMethodValidationException ex, HttpServletRequest request) {
        List<ErrorCampo> errores = new ArrayList<>();
        ex.getBeanResults().forEach(cuerpo -> {
            cuerpo.getFieldErrors()
                    .forEach(error -> errores.add(new ErrorCampo(error.getField(), error.getDefaultMessage())));
            cuerpo.getGlobalErrors()
                    .forEach(error -> errores.add(new ErrorCampo(error.getObjectName(), error.getDefaultMessage())));
        });
        ex.getValueResults().forEach(resultado -> {
            String parametro = resultado.getMethodParameter().getParameterName();
            resultado.getResolvableErrors()
                    .forEach(error -> errores.add(new ErrorCampo(parametro, error.getDefaultMessage())));
        });
        return errorValidacion(errores, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<RespuestaError> jsonIlegible(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ValorCatalogoInvalidoException catalogo = buscarCausa(ex, ValorCatalogoInvalidoException.class);
        if (catalogo != null) {
            return respuesta(HttpStatus.BAD_REQUEST, ERROR_VALIDACION, catalogo.getMessage(), request,
                    List.of(new ErrorCampo(catalogo.getCampo(), "valores permitidos: " + catalogo.getPermitidos())));
        }
        String campo = campoConError(ex);
        if (buscarCausa(ex, DateTimeParseException.class) != null) {
            return respuesta(HttpStatus.BAD_REQUEST, ERROR_VALIDACION, "Fecha con formato inválido; usa AAAA-MM-DD",
                    request, campo == null ? null : List.of(new ErrorCampo(campo, "fecha inválida; usa AAAA-MM-DD")));
        }
        if (campo != null) {
            return respuesta(HttpStatus.BAD_REQUEST, ERROR_VALIDACION, "El campo " + campo + " tiene un tipo de dato incorrecto",
                    request, List.of(new ErrorCampo(campo, "tipo de dato incorrecto")));
        }
        return respuesta(HttpStatus.BAD_REQUEST, "JSON_INVALIDO",
                "El cuerpo de la petición falta o no es un JSON válido", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<RespuestaError> tipoDeParametro(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        boolean esFecha = LocalDate.class.equals(ex.getRequiredType());
        String mensaje = esFecha ? "fecha inválida; usa AAAA-MM-DD" : "valor inválido";
        return respuesta(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO",
                "El parámetro '" + ex.getName() + "' tiene un valor inválido", request,
                List.of(new ErrorCampo(ex.getName(), mensaje)));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<RespuestaError> parametroFaltante(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, "PARAMETRO_FALTANTE",
                "Falta el parámetro obligatorio '" + ex.getParameterName() + "'", request,
                List.of(new ErrorCampo(ex.getParameterName(), "parámetro obligatorio")));
    }

    // ------------------------------------------------------------- persistencia

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<RespuestaError> integridad(DataIntegrityViolationException ex, HttpServletRequest request) {
        ServerErrorMessage error = errorPostgres(ex);
        String restriccion = error == null ? null : error.getConstraint();
        ReglaBd regla = restriccion == null ? null : RESTRICCIONES_BD.get(restriccion);
        if (regla != null) {
            return respuesta(regla.estatus(), regla.codigo(), regla.mensaje(), request, null);
        }
        log.warn("Violación de integridad no mapeada: restriccion={}, sqlState={}",
                restriccion, error == null ? null : error.getSQLState());
        if (restriccion != null && restriccion.startsWith("ck_")) {
            return respuesta(HttpStatus.BAD_REQUEST, ERROR_VALIDACION,
                    "Los datos no cumplen una regla de validación de la base de datos", request, null);
        }
        return respuesta(HttpStatus.CONFLICT, "INTEGRIDAD_DATOS",
                "La operación viola una regla de integridad de los datos", request, null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<RespuestaError> concurrencia(OptimisticLockingFailureException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.CONFLICT, "CONFLICTO_CONCURRENCIA",
                "El registro fue modificado por otra operación al mismo tiempo; consúltalo de nuevo e intenta otra vez",
                request, null);
    }

    // ---------------------------------------------------------------- seguridad

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<RespuestaError> noAutenticado(AuthenticationException ex, HttpServletRequest request) {
        String codigo;
        String detalle;
        if (ex instanceof TokenRechazadoException) {
            codigo = "TOKEN_RECHAZADO";
            detalle = ex.getMessage();
        } else if (ex instanceof OAuth2AuthenticationException) {
            codigo = "TOKEN_INVALIDO";
            detalle = "El token es inválido o ya expiró; inicia sesión de nuevo";
        } else {
            codigo = "NO_AUTENTICADO";
            detalle = "Se requiere autenticación: envía el encabezado Authorization: Bearer <token>";
        }
        HttpHeaders encabezados = new HttpHeaders();
        encabezados.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return respuesta(HttpStatus.UNAUTHORIZED, codigo, detalle, request, null, encabezados);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<RespuestaError> accesoDenegado(AccessDeniedException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO",
                "No tienes permiso para realizar esta operación", request, null);
    }

    // ------------------------------------------------------------------ general

    @ExceptionHandler(Exception.class)
    ResponseEntity<RespuestaError> general(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorSpring) {
            // Errores estándar de Spring MVC: ruta inexistente, método no permitido, tipo de contenido...
            HttpStatusCode estatus = errorSpring.getStatusCode();
            String codigo = switch (estatus.value()) {
                case 404 -> "RECURSO_NO_ENCONTRADO";
                case 405 -> "METODO_NO_PERMITIDO";
                case 406 -> "FORMATO_NO_ACEPTABLE";
                case 415 -> "TIPO_CONTENIDO_NO_SOPORTADO";
                default -> "SOLICITUD_INVALIDA";
            };
            String detalle = switch (estatus.value()) {
                case 404 -> "No existe el recurso " + request.getRequestURI();
                case 405 -> "El método " + request.getMethod() + " no está permitido en " + request.getRequestURI();
                case 415 -> "Envía el cuerpo de la petición como application/json";
                default -> titulo(estatus);
            };
            return respuesta(estatus, codigo, detalle, request, null, errorSpring.getHeaders());
        }
        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
                "Ocurrió un error inesperado. Reporta el requestId al área de soporte.", request, null);
    }

    // ---------------------------------------------------------------- utilerías

    private ResponseEntity<RespuestaError> errorValidacion(List<ErrorCampo> errores, HttpServletRequest request) {
        errores.sort(Comparator.comparing(ErrorCampo::campo).thenComparing(ErrorCampo::mensaje));
        return respuesta(HttpStatus.BAD_REQUEST, ERROR_VALIDACION,
                "La solicitud tiene " + errores.size() + " error(es) de validación", request, errores);
    }

    private ResponseEntity<RespuestaError> respuesta(HttpStatusCode estatus, String codigo, String detalle,
                                                     HttpServletRequest request, List<ErrorCampo> errores) {
        return respuesta(estatus, codigo, detalle, request, errores, new HttpHeaders());
    }

    private ResponseEntity<RespuestaError> respuesta(HttpStatusCode estatus, String codigo, String detalle,
                                                     HttpServletRequest request, List<ErrorCampo> errores,
                                                     HttpHeaders encabezados) {
        RespuestaError cuerpo = new RespuestaError(
                titulo(estatus),
                estatus.value(),
                detalle,
                request.getRequestURI(),
                codigo,
                OffsetDateTime.now(ZoneOffset.UTC),
                MDC.get(FiltroIdSolicitud.CLAVE_MDC),
                errores == null || errores.isEmpty() ? null : List.copyOf(errores));
        return ResponseEntity.status(estatus)
                .headers(encabezados)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(cuerpo);
    }

    private static String titulo(HttpStatusCode estatus) {
        return switch (estatus.value()) {
            case 400 -> "Solicitud inválida";
            case 401 -> "No autenticado";
            case 403 -> "Acceso denegado";
            case 404 -> "No encontrado";
            case 405 -> "Método no permitido";
            case 406 -> "Formato no aceptable";
            case 409 -> "Conflicto";
            case 415 -> "Tipo de contenido no soportado";
            case 429 -> "Demasiadas solicitudes";
            default -> estatus.is5xxServerError() ? "Error interno del servidor" : "Error en la solicitud";
        };
    }

    private static <T extends Throwable> T buscarCausa(Throwable ex, Class<T> tipo) {
        for (Throwable actual = ex; actual != null; actual = actual.getCause() == actual ? null : actual.getCause()) {
            if (tipo.isInstance(actual)) {
                return tipo.cast(actual);
            }
        }
        return null;
    }

    /** Nombre del campo del JSON que no se pudo leer, p. ej. "domicilio.estadoId". */
    private static String campoConError(Throwable ex) {
        for (Throwable actual = ex; actual != null; actual = actual.getCause() == actual ? null : actual.getCause()) {
            String mensaje = actual.getMessage();
            if (mensaje != null && mensaje.contains("reference chain")) {
                Matcher coincidencia = CAMPO_JSON.matcher(mensaje.substring(mensaje.indexOf("reference chain")));
                List<String> ruta = new ArrayList<>();
                while (coincidencia.find()) {
                    ruta.add(coincidencia.group(1));
                }
                if (!ruta.isEmpty()) {
                    return String.join(".", ruta);
                }
            }
        }
        return null;
    }

    private static ServerErrorMessage errorPostgres(Throwable ex) {
        for (Throwable actual = ex; actual != null; actual = actual.getCause() == actual ? null : actual.getCause()) {
            if (actual instanceof PSQLException psql) {
                return psql.getServerErrorMessage();
            }
            if (actual instanceof SQLException sql) {
                for (SQLException siguiente = sql.getNextException(); siguiente != null; siguiente = siguiente.getNextException()) {
                    if (siguiente instanceof PSQLException psql) {
                        return psql.getServerErrorMessage();
                    }
                }
            }
        }
        return null;
    }

    private record ReglaBd(HttpStatus estatus, String codigo, String mensaje) {
    }
}
