package com.example.banco.integracion;

import com.example.banco.entity.enums.CatalogoEnum;
import com.example.banco.entity.enums.EstadoCivil;
import com.example.banco.entity.enums.EstatusCuenta;
import com.example.banco.entity.enums.Sexo;
import com.example.banco.soporte.PostgresEmbebido;
import com.example.banco.soporte.PruebaIntegracion;
import com.example.banco.util.Constantes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La base de datos protege las reglas de negocio por sí misma (CHECK, UNIQUE,
 * triggers y permisos), y el script entregable produce exactamente el mismo
 * esquema que las migraciones de la aplicación.
 */
@DisplayName("Base de datos · reglas, script de creación y consistencia con Java")
class BaseDeDatosIT extends PruebaIntegracion {

    private static Connection conexion(String baseDeDatos) throws SQLException {
        // Modo de consulta simple: el servidor ejecuta el script completo tal cual (bloques $$, BEGIN/COMMIT...)
        return DriverManager.getConnection(PostgresEmbebido.jdbcUrl(baseDeDatos) + "&preferQueryMode=simple");
    }

    private static String leer(String ruta) throws Exception {
        return Files.readString(Path.of(ruta), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Las 62 pruebas SQL de reglas de negocio pasan (database/pruebas_reglas_bd.sql)")
    void reglasDeNegocioEnLaBaseDeDatos() throws Exception {
        List<String> avisos = new ArrayList<>();
        try (Connection conexion = conexion("postgres"); Statement sentencia = conexion.createStatement()) {
            sentencia.execute(leer("database/pruebas_reglas_bd.sql"));
            for (SQLWarning aviso = sentencia.getWarnings(); aviso != null; aviso = aviso.getNextWarning()) {
                avisos.add(aviso.getMessage());
            }
        }
        avisos.forEach(System.out::println);
        assertThat(avisos).noneMatch(aviso -> aviso.startsWith("FALLA"));
        assertThat(avisos).anyMatch(aviso -> aviso.matches("==== \\d+ pruebas, \\d+ correctas, 0 fallidas ===="));
    }

    @Test
    @DisplayName("database/schema.sql crea un esquema idéntico al de las migraciones Flyway")
    void scriptEquivalenteAMigraciones() throws Exception {
        try (Connection admin = conexion("postgres"); Statement sentencia = admin.createStatement()) {
            sentencia.execute("DROP DATABASE IF EXISTS verificacion_script");
            sentencia.execute("CREATE DATABASE verificacion_script ENCODING 'UTF8' TEMPLATE template0");
        }
        try (Connection script = conexion("verificacion_script"); Statement sentencia = script.createStatement()) {
            sentencia.execute(leer("database/schema.sql"));
        }

        List<String> esperado;
        List<String> obtenido;
        try (Connection flyway = conexion("postgres"); Connection script = conexion("verificacion_script")) {
            esperado = huella(flyway);
            obtenido = huella(script);
        }
        assertThat(obtenido).isNotEmpty().containsExactlyElementsOf(esperado);
    }

    @Test
    @DisplayName("Los enums de Java coinciden con los catálogos de la base de datos (id y clave)")
    void enumsCoincidenConCatalogos() {
        verificarCatalogo("cat_sexo", Sexo.values());
        verificarCatalogo("cat_estado_civil", EstadoCivil.values());
        verificarCatalogo("cat_estatus_cuenta", EstatusCuenta.values());
    }

    @ParameterizedTest(name = "{0} \"{1}\"")
    @CsvSource(delimiter = '|', value = {
            "curp   | LOHF950821MGTPRR08",
            "curp   | LOHF950821MXXPRR08",
            "curp   | LOHF951321MGTPRR08",
            "curp   | lohf950821mgtprr08",
            "rfc    | LOHF950821QK5",
            "rfc    | LOH950821QK5",
            "rfc    | LOHF950821QKB",
            "rfc    | LOHF950800QK5",
            "correo | maria.lopez@ejemplo.com",
            "correo | maria..lopez@ejemplo.com",
            "correo | maria@ejemplo",
            "correo | a+b_c%d-e@sub.dominio.com.mx"
    })
    @DisplayName("Java y PostgreSQL validan los formatos exactamente igual")
    void mismaValidacionEnJavaYPostgres(String tipo, String valor) {
        String regex = switch (tipo) {
            case "curp" -> Constantes.REGEX_CURP;
            case "rfc" -> Constantes.REGEX_RFC;
            default -> Constantes.REGEX_CORREO;
        };
        String restriccion = switch (tipo) {
            case "curp" -> "ck_clientes_curp";
            case "rfc" -> "ck_clientes_rfc";
            default -> "ck_clientes_correo";
        };
        boolean java = Pattern.matches(regex, valor);
        // Extrae la expresión del CHECK real de la tabla y la evalúa en PostgreSQL
        String definicion = jdbc.queryForObject(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = ?", String.class, restriccion);
        String expresionPg = definicion.substring(definicion.indexOf("~ '") + 3, definicion.lastIndexOf("'::text"));
        Boolean postgres = jdbc.queryForObject("SELECT ?::text ~ ?::text", Boolean.class, valor, expresionPg);
        assertThat(postgres).isEqualTo(java);
    }

    private void verificarCatalogo(String tabla, CatalogoEnum[] valores) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM onboarding." + tabla, Integer.class);
        assertThat(total).isEqualTo(valores.length);
        for (CatalogoEnum valor : valores) {
            String clave = jdbc.queryForObject("SELECT clave FROM onboarding." + tabla + " WHERE id = ?",
                    String.class, valor.getId());
            assertThat(clave).isEqualTo(((Enum<?>) valor).name());
        }
    }

    /** Descripción textual del esquema: columnas, restricciones, índices, funciones, triggers y catálogos. */
    private static List<String> huella(Connection conexion) throws SQLException {
        List<String> lineas = new ArrayList<>();
        String[] consultas = {
                "SELECT 'col ' || table_name || '.' || column_name || ' ' || data_type || ' ' || coalesce(character_maximum_length::text, '') "
                        + "|| ' ' || coalesce(numeric_precision::text, '') || ',' || coalesce(numeric_scale::text, '') || ' ' || is_nullable "
                        + "|| ' ' || coalesce(column_default, '') FROM information_schema.columns "
                        + "WHERE table_schema = 'onboarding' AND table_name <> 'flyway_schema_history' ORDER BY 1",
                "SELECT 'con ' || c.conname || ' ' || pg_get_constraintdef(c.oid) FROM pg_constraint c "
                        + "JOIN pg_namespace n ON n.oid = c.connamespace WHERE n.nspname = 'onboarding' "
                        + "AND c.conrelid::regclass::text NOT LIKE '%flyway_schema_history%' ORDER BY 1",
                "SELECT 'idx ' || indexname || ' ' || indexdef FROM pg_indexes WHERE schemaname = 'onboarding' "
                        + "AND tablename <> 'flyway_schema_history' ORDER BY 1",
                "SELECT 'fun ' || p.proname || ' ' || md5(p.prosrc) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname = 'onboarding' ORDER BY 1",
                "SELECT 'trg ' || t.tgname || ' ' || pg_get_triggerdef(t.oid) FROM pg_trigger t JOIN pg_class c ON c.oid = t.tgrelid "
                        + "JOIN pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = 'onboarding' AND NOT t.tgisinternal ORDER BY 1",
                "SELECT 'cat ' || (SELECT count(*) FROM onboarding.cat_pais) || ' ' || (SELECT count(*) FROM onboarding.cat_estado) "
                        + "|| ' ' || (SELECT count(*) FROM onboarding.cat_sexo) || ' ' || (SELECT count(*) FROM onboarding.cat_estado_civil) "
                        + "|| ' ' || (SELECT count(*) FROM onboarding.cat_estatus_cuenta)"
        };
        try (Statement sentencia = conexion.createStatement()) {
            for (String consulta : consultas) {
                try (ResultSet filas = sentencia.executeQuery(consulta)) {
                    while (filas.next()) {
                        lineas.add(filas.getString(1));
                    }
                }
            }
        }
        return lineas;
    }
}
