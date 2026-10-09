package com.example.banco.security;

import com.example.banco.entity.Cliente;
import com.example.banco.entity.Usuario;
import com.example.banco.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Conversión del JWT en el usuario autenticado")
class ConvertidorJwtUsuarioTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private ConvertidorJwtUsuario convertidor;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        convertidor = new ConvertidorJwtUsuario(usuarioRepository);
        Cliente cliente = new Cliente("GOMC900515HQTMNR08", "GOMC900515AB7");
        cliente.setCorreo("carlos@prueba.mx");
        usuario = Usuario.crear(cliente, "$2a$04$hash");
        ReflectionTestUtils.setField(usuario, "id", 7);
        ReflectionTestUtils.setField(usuario, "clienteId", 11);
    }

    private static Jwt jwt(String sujeto, Object version) {
        Jwt.Builder constructor = Jwt.withTokenValue("token").header("alg", "HS256").subject(sujeto)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        if (version != null) {
            constructor.claim(JwtService.CLAIM_VERSION_CREDENCIALES, version);
        }
        return constructor.build();
    }

    @Test
    @DisplayName("Usuario activo con la versión de credenciales vigente: queda autenticado")
    void autentica() {
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario));

        AbstractAuthenticationToken autenticacion = convertidor.convert(jwt("7", 0L));

        assertThat(autenticacion.isAuthenticated()).isTrue();
        assertThat(autenticacion.getPrincipal()).isEqualTo(new UsuarioAutenticado(7, 11, "carlos@prueba.mx"));
        assertThat(autenticacion.getName()).isEqualTo("7");
        assertThat(autenticacion.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_CLIENTE");
    }

    @Test
    @DisplayName("Usuario inactivo (cliente dado de baja): token rechazado")
    void inactivo() {
        usuario.inactivar();
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario));
        assertThatThrownBy(() -> convertidor.convert(jwt("7", 0L)))
                .isInstanceOf(TokenRechazadoException.class).hasMessageContaining("inactivo");
    }

    @Test
    @DisplayName("Token emitido antes de un cambio de contraseña: rechazado")
    void contrasenaCambiada() {
        usuario.cambiarPassword("$2a$04$nuevo");
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario));
        assertThatThrownBy(() -> convertidor.convert(jwt("7", 0L)))
                .isInstanceOf(TokenRechazadoException.class).hasMessageContaining("contraseña");
        assertThatThrownBy(() -> convertidor.convert(jwt("7", null))).isInstanceOf(TokenRechazadoException.class);
    }

    @Test
    @DisplayName("Usuario inexistente o sujeto no numérico: rechazado")
    void sujetoInvalido() {
        when(usuarioRepository.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> convertidor.convert(jwt("99", 0L))).isInstanceOf(TokenRechazadoException.class);
        assertThatThrownBy(() -> convertidor.convert(jwt("abc", 0L))).isInstanceOf(TokenRechazadoException.class);
    }
}
