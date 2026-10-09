package com.example.banco.service;

import com.example.banco.dto.request.LoginRequest;
import com.example.banco.dto.response.LoginResponse;
import com.example.banco.entity.Cliente;
import com.example.banco.entity.Usuario;
import com.example.banco.exception.CredencialesInvalidasException;
import com.example.banco.exception.DemasiadosIntentosException;
import com.example.banco.exception.UsuarioInactivoException;
import com.example.banco.repository.UsuarioRepository;
import com.example.banco.security.IntentosLoginService;
import com.example.banco.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService: inicio de sesión")
class AuthServiceTest {

    private static final String CORREO = "carlos@prueba.mx";
    private static final String PASSWORD = "Segura#2026";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private IntentosLoginService intentosLogin;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private AuthService servicio;

    @BeforeEach
    void crear() {
        servicio = new AuthService(usuarioRepository, passwordEncoder, jwtService, intentosLogin);
    }

    private Usuario usuario(boolean activo) {
        Cliente cliente = new Cliente("GOMC900515HQTMNR08", "GOMC900515AB7");
        cliente.setCorreo(CORREO);
        Usuario usuario = Usuario.crear(cliente, passwordEncoder.encode(PASSWORD));
        ReflectionTestUtils.setField(usuario, "id", 3);
        ReflectionTestUtils.setField(usuario, "clienteId", 5);
        if (!activo) {
            usuario.inactivar();
        }
        return usuario;
    }

    @Test
    @DisplayName("Credenciales correctas de un usuario activo: emite el JWT")
    void loginCorrecto() {
        Usuario usuario = usuario(true);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(jwtService.emitir(usuario)).thenReturn(new JwtService.TokenEmitido("jwt", Instant.now(), 3600));

        LoginResponse respuesta = servicio.iniciarSesion(new LoginRequest(" Carlos@Prueba.MX ", PASSWORD));

        assertThat(respuesta.token()).isEqualTo("jwt");
        assertThat(respuesta.tipo()).isEqualTo("Bearer");
        assertThat(respuesta.expiraEnSegundos()).isEqualTo(3600);
        assertThat(respuesta.usuarioId()).isEqualTo(3);
        assertThat(respuesta.clienteId()).isEqualTo(5);
        verify(intentosLogin).reiniciar(CORREO);
    }

    @Test
    @DisplayName("Contraseña incorrecta: credenciales inválidas y se cuenta el intento fallido")
    void contrasenaIncorrecta() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario(true)));

        assertThatThrownBy(() -> servicio.iniciarSesion(new LoginRequest(CORREO, "Otra#2026x")))
                .isInstanceOf(CredencialesInvalidasException.class);
        verify(intentosLogin).registrarFallo(CORREO);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Correo no registrado: el mismo error que una contraseña incorrecta")
    void correoInexistente() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciarSesion(new LoginRequest(CORREO, PASSWORD)))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Correo o contraseña incorrectos");
        verify(intentosLogin).registrarFallo(CORREO);
    }

    @Test
    @DisplayName("Usuario inactivo con contraseña correcta: acceso denegado")
    void usuarioInactivo() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario(false)));

        assertThatThrownBy(() -> servicio.iniciarSesion(new LoginRequest(CORREO, PASSWORD)))
                .isInstanceOf(UsuarioInactivoException.class);
        verifyNoInteractions(jwtService);
        verify(intentosLogin, never()).reiniciar(any());
    }

    @Test
    @DisplayName("Correo bloqueado por intentos fallidos: se rechaza antes de revisar la contraseña")
    void bloqueado() {
        doThrow(new DemasiadosIntentosException(600)).when(intentosLogin).verificarNoBloqueado(CORREO);

        assertThatThrownBy(() -> servicio.iniciarSesion(new LoginRequest(CORREO, PASSWORD)))
                .isInstanceOf(DemasiadosIntentosException.class);
        verifyNoInteractions(usuarioRepository, jwtService);
    }
}
