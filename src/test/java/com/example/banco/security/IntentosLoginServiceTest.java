package com.example.banco.security;

import com.example.banco.config.SeguridadProperties;
import com.example.banco.exception.DemasiadosIntentosException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Bloqueo por intentos fallidos de inicio de sesión")
class IntentosLoginServiceTest {

    private RelojAjustable reloj;
    private IntentosLoginService intentos;

    @BeforeEach
    void preparar() {
        reloj = new RelojAjustable(Instant.parse("2026-10-06T15:00:00Z"));
        SeguridadProperties propiedades = new SeguridadProperties(4,
                new SeguridadProperties.Jwt("x".repeat(32), "e", "a", Duration.ofMinutes(60)),
                new SeguridadProperties.Login(3, Duration.ofMinutes(10)),
                new SeguridadProperties.Cors(List.of()));
        intentos = new IntentosLoginService(propiedades, reloj);
    }

    @Test
    @DisplayName("Bloquea el correo al llegar al máximo de intentos fallidos")
    void bloquea() {
        intentos.registrarFallo("a@prueba.mx");
        intentos.registrarFallo("a@prueba.mx");
        assertThatCode(() -> intentos.verificarNoBloqueado("a@prueba.mx")).doesNotThrowAnyException();

        intentos.registrarFallo("a@prueba.mx");

        assertThatThrownBy(() -> intentos.verificarNoBloqueado("a@prueba.mx"))
                .isInstanceOfSatisfying(DemasiadosIntentosException.class,
                        ex -> assertThat(ex.getSegundosParaReintentar()).isBetween(590L, 601L));
    }

    @Test
    @DisplayName("El bloqueo termina cuando pasa el tiempo configurado")
    void expira() {
        for (int i = 0; i < 3; i++) {
            intentos.registrarFallo("a@prueba.mx");
        }
        reloj.avanzar(Duration.ofMinutes(11));
        assertThatCode(() -> intentos.verificarNoBloqueado("a@prueba.mx")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Un inicio de sesión correcto reinicia el contador; cada correo cuenta por separado")
    void reiniciaYSepara() {
        intentos.registrarFallo("a@prueba.mx");
        intentos.registrarFallo("a@prueba.mx");
        intentos.reiniciar("a@prueba.mx");
        intentos.registrarFallo("a@prueba.mx");
        for (int i = 0; i < 3; i++) {
            intentos.registrarFallo("b@prueba.mx");
        }
        assertThatCode(() -> intentos.verificarNoBloqueado("a@prueba.mx")).doesNotThrowAnyException();
        assertThatThrownBy(() -> intentos.verificarNoBloqueado("b@prueba.mx")).isInstanceOf(DemasiadosIntentosException.class);
    }

    /** Reloj controlable para simular el paso del tiempo. */
    static final class RelojAjustable extends Clock {
        private Instant ahora;

        RelojAjustable(Instant inicio) {
            this.ahora = inicio;
        }

        void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}
