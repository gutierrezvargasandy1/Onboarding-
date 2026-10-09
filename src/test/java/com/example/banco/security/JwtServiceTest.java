package com.example.banco.security;

import com.example.banco.config.JwtConfig;
import com.example.banco.config.SeguridadProperties;
import com.example.banco.entity.Cliente;
import com.example.banco.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JWT: emisión y validación")
class JwtServiceTest {

    private static final String SECRETO = "secreto-solo-para-pruebas-automatizadas-0123456789";
    private static final String EMISOR = "banco-onboarding";
    private static final String AUDIENCIA = "banco-onboarding-api";

    private final JwtConfig config = new JwtConfig();
    private JwtDecoder decoder;
    private Usuario usuario;

    private static SeguridadProperties propiedades(String secreto, String emisor, String audiencia) {
        return new SeguridadProperties(4,
                new SeguridadProperties.Jwt(secreto, emisor, audiencia, Duration.ofMinutes(60)),
                new SeguridadProperties.Login(3, Duration.ofMinutes(10)),
                new SeguridadProperties.Cors(List.of()));
    }

    private JwtService servicio(SeguridadProperties propiedades, Clock reloj) {
        SecretKey clave = config.claveJwt(propiedades);
        return new JwtService(config.jwtEncoder(clave), propiedades, reloj);
    }

    @BeforeEach
    void preparar() {
        SeguridadProperties propiedades = propiedades(SECRETO, EMISOR, AUDIENCIA);
        decoder = config.jwtDecoder(config.claveJwt(propiedades), propiedades);
        Cliente cliente = new Cliente("GOMC900515HQTMNR08", "GOMC900515AB7");
        cliente.setCorreo("carlos@prueba.mx");
        usuario = Usuario.crear(cliente, "$2a$04$hash");
        ReflectionTestUtils.setField(usuario, "id", 42);
    }

    @Test
    @DisplayName("El token lleva el id del usuario, emisor, audiencia, vigencia de 60 min y versión de credenciales")
    void contenidoDelToken() {
        JwtService.TokenEmitido token = servicio(propiedades(SECRETO, EMISOR, AUDIENCIA), Clock.systemUTC()).emitir(usuario);

        Jwt jwt = decoder.decode(token.valor());

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(EMISOR);
        assertThat(jwt.getAudience()).containsExactly(AUDIENCIA);
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(60));
        assertThat(((Number) jwt.getClaim(JwtService.CLAIM_VERSION_CREDENCIALES)).intValue()).isZero();
        assertThat(jwt.getId()).isNotBlank();
        assertThat(token.segundosVigencia()).isEqualTo(3600);
        assertThat(token.valor()).doesNotContain("carlos");
    }

    @Test
    @DisplayName("Rechaza un token vencido")
    void tokenVencido() {
        Clock haceDosHoras = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
        String token = servicio(propiedades(SECRETO, EMISOR, AUDIENCIA), haceDosHoras).emitir(usuario).valor();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Rechaza un token firmado con otra clave")
    void otraClave() {
        String token = servicio(propiedades("otra-clave-distinta-de-al-menos-32-caracteres!!", EMISOR, AUDIENCIA),
                Clock.systemUTC()).emitir(usuario).valor();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Rechaza un token alterado")
    void tokenAlterado() {
        String token = servicio(propiedades(SECRETO, EMISOR, AUDIENCIA), Clock.systemUTC()).emitir(usuario).valor();
        String alterado = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");

        assertThatThrownBy(() -> decoder.decode(alterado)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Rechaza tokens de otro emisor o para otra audiencia")
    void emisorYAudiencia() {
        String otroEmisor = servicio(propiedades(SECRETO, "otro-sistema", AUDIENCIA), Clock.systemUTC()).emitir(usuario).valor();
        String otraAudiencia = servicio(propiedades(SECRETO, EMISOR, "otra-api"), Clock.systemUTC()).emitir(usuario).valor();

        assertThatThrownBy(() -> decoder.decode(otroEmisor)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decoder.decode(otraAudiencia)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("No arranca con un secreto de menos de 32 bytes")
    void secretoCorto() {
        assertThatThrownBy(() -> config.claveJwt(propiedades("corto", EMISOR, AUDIENCIA)))
                .isInstanceOf(IllegalStateException.class);
    }
}
