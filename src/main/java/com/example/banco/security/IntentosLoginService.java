package com.example.banco.security;

import com.example.banco.config.SeguridadProperties;
import com.example.banco.exception.DemasiadosIntentosException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Protección contra fuerza bruta: tras N intentos fallidos con el mismo correo,
 * ese correo queda bloqueado durante un tiempo. Se guarda en memoria con límite
 * de tamaño (si la API corre en varias instancias, esto iría en Redis).
 */
@Service
public class IntentosLoginService {

    private final int maxIntentos;
    private final Duration bloqueo;
    private final Clock reloj;
    private final Cache<String, Intentos> intentos;

    public IntentosLoginService(SeguridadProperties seguridad, Clock reloj) {
        this.maxIntentos = seguridad.login().maxIntentos();
        this.bloqueo = seguridad.login().bloqueo();
        this.reloj = reloj;
        this.intentos = Caffeine.newBuilder()
                .expireAfterWrite(bloqueo)
                .maximumSize(100_000)
                .build();
    }

    public void verificarNoBloqueado(String correo) {
        Intentos actual = intentos.getIfPresent(correo);
        Instant ahora = reloj.instant();
        if (actual != null && actual.bloqueadoHasta() != null && actual.bloqueadoHasta().isAfter(ahora)) {
            throw new DemasiadosIntentosException(Duration.between(ahora, actual.bloqueadoHasta()).toSeconds() + 1);
        }
    }

    public void registrarFallo(String correo) {
        intentos.asMap().compute(correo, (clave, actual) -> {
            int fallos = (actual == null ? 0 : actual.fallos()) + 1;
            Instant hasta = fallos >= maxIntentos ? reloj.instant().plus(bloqueo) : null;
            return new Intentos(fallos, hasta);
        });
    }

    public void reiniciar(String correo) {
        intentos.invalidate(correo);
    }

    private record Intentos(int fallos, Instant bloqueadoHasta) {
    }
}
