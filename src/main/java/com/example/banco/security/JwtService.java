package com.example.banco.security;

import com.example.banco.config.SeguridadProperties;
import com.example.banco.entity.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Emite los JWT firmados con HMAC-SHA256. El token solo lleva el id del usuario
 * (sin datos personales), su vigencia y la versión de credenciales.
 */
@Service
public class JwtService {

    /** Versión de credenciales del usuario al momento del login. */
    public static final String CLAIM_VERSION_CREDENCIALES = "ver";

    private final JwtEncoder jwtEncoder;
    private final SeguridadProperties.Jwt configuracion;
    private final Clock reloj;

    public JwtService(JwtEncoder jwtEncoder, SeguridadProperties seguridad, Clock reloj) {
        this.jwtEncoder = jwtEncoder;
        this.configuracion = seguridad.jwt();
        this.reloj = reloj;
    }

    public TokenEmitido emitir(Usuario usuario) {
        Instant ahora = reloj.instant();
        Instant expira = ahora.plus(configuracion.expiracion());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(configuracion.emisor())
                .audience(List.of(configuracion.audiencia()))
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(ahora)
                .expiresAt(expira)
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_VERSION_CREDENCIALES, usuario.getVersionCredenciales())
                .build();
        JwsHeader encabezado = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = jwtEncoder.encode(JwtEncoderParameters.from(encabezado, claims)).getTokenValue();
        return new TokenEmitido(valor, expira, configuracion.expiracion().toSeconds());
    }

    public record TokenEmitido(String valor, Instant expiraEn, long segundosVigencia) {
    }
}
