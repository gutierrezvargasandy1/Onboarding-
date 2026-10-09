package com.example.banco.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Se superó el máximo de intentos fallidos de inicio de sesión; el correo queda bloqueado un tiempo. */
@Getter
public class DemasiadosIntentosException extends NegocioException {

    private final long segundosParaReintentar;

    public DemasiadosIntentosException(long segundosParaReintentar) {
        super(HttpStatus.TOO_MANY_REQUESTS, "DEMASIADOS_INTENTOS",
                "Demasiados intentos fallidos. Intenta de nuevo en " + Math.max(1, (segundosParaReintentar + 59) / 60) + " minuto(s)");
        this.segundosParaReintentar = segundosParaReintentar;
    }
}
