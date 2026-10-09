package com.example.banco.soporte;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * PostgreSQL real (binarios oficiales) que se inicia una sola vez por ejecución de
 * pruebas. No requiere Docker ni una instalación previa de PostgreSQL.
 */
public final class PostgresEmbebido {

    private static EmbeddedPostgres servidor;

    private PostgresEmbebido() {
    }

    public static synchronized EmbeddedPostgres servidor() {
        if (servidor == null) {
            try {
                servidor = EmbeddedPostgres.builder().start();
            } catch (IOException e) {
                throw new UncheckedIOException("No se pudo iniciar PostgreSQL embebido", e);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    servidor.close();
                } catch (IOException ignorada) {
                    // la JVM está terminando
                }
            }));
        }
        return servidor;
    }

    public static String jdbcUrl(String baseDeDatos) {
        return servidor().getJdbcUrl("postgres", baseDeDatos);
    }
}
