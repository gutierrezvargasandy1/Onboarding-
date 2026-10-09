package com.example.banco.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Collection;

/**
 * JWT firmados con HMAC-SHA256 usando el soporte estándar de Spring Security
 * (Nimbus). El decodificador valida firma, vigencia, emisor y audiencia.
 */
@Configuration
public class JwtConfig {

    @Bean
    public SecretKey claveJwt(SeguridadProperties seguridad) {
        byte[] bytes = seguridad.jwt().secreto().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("El secreto JWT debe tener al menos 32 bytes (256 bits)");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey claveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveJwt));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey claveJwt, SeguridadProperties seguridad) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(claveJwt)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        String audiencia = seguridad.jwt().audiencia();
        OAuth2TokenValidator<Jwt> validarAudiencia = new JwtClaimValidator<Collection<String>>(
                JwtClaimNames.AUD, valor -> valor != null && valor.contains(audiencia));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(seguridad.jwt().emisor()), validarAudiencia));
        return decoder;
    }

    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }
}
