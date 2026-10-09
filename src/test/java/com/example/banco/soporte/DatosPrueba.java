package com.example.banco.soporte;

import com.example.banco.dto.request.ClienteRequest;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Genera clientes válidos y únicos (CURP, RFC y correo distintos en cada llamada).
 * Se usan mapas para poder mandar a la API valores inválidos o campos faltantes.
 */
public final class DatosPrueba {

    public static final String PASSWORD = "Segura#2026";
    public static final LocalDate FECHA_NACIMIENTO = LocalDate.of(1990, 5, 15);

    private static final AtomicInteger CONTADOR = new AtomicInteger();
    private static final String CONSONANTES = "BCDFGHJKLMNPQRSTVWXYZ";
    private static final String ALFANUMERICOS = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789";
    private static final DateTimeFormatter AAMMDD = DateTimeFormatter.ofPattern("yyMMdd");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private DatosPrueba() {
    }

    public static Map<String, Object> cliente() {
        return cliente(FECHA_NACIMIENTO);
    }

    public static Map<String, Object> cliente(LocalDate fechaNacimiento) {
        int n = CONTADOR.incrementAndGet();
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", "Carlos");
        datos.put("segundoNombre", "Alberto");
        datos.put("apellidoPaterno", "Gómez");
        datos.put("apellidoMaterno", "Mendoza");
        datos.put("fechaNacimiento", fechaNacimiento.toString());
        datos.put("curp", curp(fechaNacimiento, n));
        datos.put("rfc", rfc(fechaNacimiento, n));
        datos.put("sexo", "H");
        datos.put("nacionalidad", "MX");
        datos.put("estadoCivil", "CASADO");
        datos.put("correo", "cliente" + n + "@prueba.mx");
        datos.put("telefonoMovil", "4421234567");
        datos.put("telefonoAlterno", null);
        datos.put("domicilio", domicilio());
        datos.put("ocupacion", "Contador");
        datos.put("empresa", "Despacho Gómez");
        datos.put("ingresoMensual", new BigDecimal("28000.00"));
        datos.put("password", PASSWORD);
        return datos;
    }

    public static Map<String, Object> domicilio() {
        Map<String, Object> domicilio = new LinkedHashMap<>();
        domicilio.put("calle", "Av. Constituyentes");
        domicilio.put("numeroExterior", "100");
        domicilio.put("numeroInterior", null);
        domicilio.put("colonia", "Centro");
        domicilio.put("municipio", "Querétaro");
        domicilio.put("estadoId", 22);
        domicilio.put("codigoPostal", "76000");
        domicilio.put("pais", "MX");
        return domicilio;
    }

    /** Copia del cliente lista para un PUT (sin contraseña). */
    public static Map<String, Object> actualizacion(Map<String, Object> cliente) {
        Map<String, Object> datos = new LinkedHashMap<>(cliente);
        datos.remove("password");
        datos.remove("curp");
        datos.remove("rfc");
        datos.put("domicilio", new LinkedHashMap<>(castMapa(cliente.get("domicilio"))));
        return datos;
    }

    /** La misma petición convertida al record, como lo haría Jackson en el controlador. */
    public static ClienteRequest solicitud(Map<String, Object> datos) {
        return JSON.convertValue(datos, ClienteRequest.class);
    }

    public static ClienteRequest solicitud() {
        return solicitud(cliente());
    }

    public static String curp(LocalDate fecha, int n) {
        return "GOMC" + fecha.format(AAMMDD) + "HQT" + consonantes(n) + "0" + (n % 10);
    }

    public static String rfc(LocalDate fecha, int n) {
        int base = n / 10;
        return "GOMC" + fecha.format(AAMMDD)
                + ALFANUMERICOS.charAt(base / ALFANUMERICOS.length() % ALFANUMERICOS.length())
                + ALFANUMERICOS.charAt(base % ALFANUMERICOS.length())
                + (n % 10);
    }

    private static String consonantes(int n) {
        int b = CONSONANTES.length();
        return "" + CONSONANTES.charAt(n / (b * b) % b) + CONSONANTES.charAt(n / b % b) + CONSONANTES.charAt(n % b);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> castMapa(Object valor) {
        return (Map<String, Object>) valor;
    }
}
