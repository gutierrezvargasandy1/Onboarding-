package com.example.banco.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Asigna un identificador a cada petición (o reutiliza el encabezado X-Request-Id
 * si viene bien formado), lo pone en los logs (MDC) y lo devuelve en la respuesta.
 * Permite rastrear un error reportado por un cliente hasta la línea exacta del log.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroIdSolicitud extends OncePerRequestFilter {

    public static final String ENCABEZADO = "X-Request-Id";
    public static final String CLAVE_MDC = "requestId";

    /** Evita inyección en logs: solo caracteres seguros y longitud acotada. */
    private static final Pattern ID_VALIDO = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        String recibido = request.getHeader(ENCABEZADO);
        String id = recibido != null && ID_VALIDO.matcher(recibido).matches() ? recibido : UUID.randomUUID().toString();
        MDC.put(CLAVE_MDC, id);
        response.setHeader(ENCABEZADO, id);
        try {
            cadena.doFilter(request, response);
        } finally {
            MDC.remove(CLAVE_MDC);
        }
    }
}
