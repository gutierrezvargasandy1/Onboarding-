package com.example.banco;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * database/schema.sql (entregable) debe contener íntegras las migraciones Flyway.
 * Si alguien cambia una migración y olvida el script, esta prueba falla.
 */
@DisplayName("Script de creación de base de datos")
class ScriptBaseDatosTest {

    private static String leer(Path archivo) throws IOException {
        return Files.readString(archivo, StandardCharsets.UTF_8).replace("\r\n", "\n").strip();
    }

    @Test
    @DisplayName("schema.sql contiene íntegras las migraciones V1, V2 y V3 dentro de una transacción")
    void scriptEquivaleALasMigraciones() throws IOException {
        String script = leer(Path.of("database", "schema.sql"));
        List<Path> migraciones;
        try (Stream<Path> archivos = Files.list(Path.of("src", "main", "resources", "db", "migration"))) {
            migraciones = archivos.filter(archivo -> archivo.getFileName().toString().matches("V\\d+__.*\\.sql")).sorted().toList();
        }

        assertThat(migraciones).hasSize(3);
        for (Path migracion : migraciones) {
            assertThat(script).as("schema.sql debe incluir %s", migracion.getFileName()).contains(leer(migracion));
        }
        assertThat(script).contains("BEGIN;").endsWith("COMMIT;");
    }
}
