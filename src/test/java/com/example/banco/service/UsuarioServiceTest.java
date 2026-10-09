package com.example.banco.service;

import com.example.banco.dto.request.CambioPasswordRequest;
import com.example.banco.dto.response.UsuarioResponse;
import com.example.banco.entity.Cliente;
import com.example.banco.entity.Usuario;
import com.example.banco.exception.AccesoDenegadoException;
import com.example.banco.exception.PasswordInvalidaException;
import com.example.banco.exception.UsuarioInactivoException;
import com.example.banco.exception.UsuarioNoEncontradoException;
import com.example.banco.mapper.UsuarioMapper;
import com.example.banco.repository.ClienteRepository;
import com.example.banco.repository.UsuarioRepository;
import com.example.banco.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService: usuario de acceso y contraseñas")
class UsuarioServiceTest {

    private static final String ACTUAL = "Segura#2026";
    private static final String NUEVA = "NuevaClave#2026";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ClienteRepository clienteRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private UsuarioService servicio;

    @BeforeEach
    void crear() {
        servicio = new UsuarioService(
                usuarioRepository,
                clienteRepository, // <--- AGREGAR EL MOCK AQUÍ
                passwordEncoder,
                new UsuarioMapper());
    }

    private Usuario usuario(int id, boolean activo) {
        Cliente cliente = new Cliente("GOMC900515HQTMNR08", "GOMC900515AB7");
        cliente.setCorreo("carlos@prueba.mx");
        Usuario usuario = Usuario.crear(cliente, passwordEncoder.encode(ACTUAL));
        ReflectionTestUtils.setField(usuario, "id", id);
        ReflectionTestUtils.setField(usuario, "clienteId", id);
        if (!activo) {
            usuario.inactivar();
        }
        return usuario;
    }

    private static UsuarioAutenticado sesion(int usuarioId) {
        return new UsuarioAutenticado(usuarioId, usuarioId, "carlos@prueba.mx");
    }

    @Test
    @DisplayName("Crea el usuario con el correo del cliente, activo y con la contraseña cifrada con BCrypt")
    void creaUsuario() {
        Cliente cliente = new Cliente("GOMC900515HQTMNR08", "GOMC900515AB7");
        cliente.setCorreo("carlos@prueba.mx");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Usuario usuario = servicio.crearUsuario(cliente, ACTUAL);

        assertThat(usuario.getCorreo()).isEqualTo("carlos@prueba.mx");
        assertThat(usuario.isActivo()).isTrue();
        assertThat(usuario.getPassword()).startsWith("$2a$").isNotEqualTo(ACTUAL);
        assertThat(passwordEncoder.matches(ACTUAL, usuario.getPassword())).isTrue();
    }

    @Test
    @DisplayName("No crea usuarios con contraseñas que no cumplen la política")
    void contrasenaDebil() {
        Cliente cliente = new Cliente("GOMC900515HQTMNR08", "GOMC900515AB7");
        assertThatThrownBy(() -> servicio.crearUsuario(cliente, "debil"))
                .isInstanceOf(PasswordInvalidaException.class)
                .hasMessageContaining("mínimo 8 caracteres");
    }

    @Test
    @DisplayName("Consulta el propio usuario sin exponer la contraseña")
    void consultaPropia() {
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario(7, true)));
        UsuarioResponse respuesta = servicio.obtener(7, sesion(7));
        assertThat(respuesta.id()).isEqualTo(7);
        assertThat(respuesta.correo()).isEqualTo("carlos@prueba.mx");
        assertThat(respuesta.toString()).doesNotContain("$2a$");
    }

    @Test
    @DisplayName("No permite consultar el usuario de otra persona; inexistente es 404")
    void propiedad() {
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario(7, true)));
        assertThatThrownBy(() -> servicio.obtener(7, sesion(8))).isInstanceOf(AccesoDenegadoException.class);
        when(usuarioRepository.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.obtener(99, sesion(8))).isInstanceOf(UsuarioNoEncontradoException.class);
    }

    @Test
    @DisplayName("Cambia la contraseña: guarda el nuevo hash y sube la versión de credenciales")
    void cambiaContrasena() {
        Usuario usuario = usuario(7, true);
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario));

        servicio.cambiarPassword(7, new CambioPasswordRequest(ACTUAL, NUEVA), sesion(7));

        assertThat(passwordEncoder.matches(NUEVA, usuario.getPassword())).isTrue();
        assertThat(passwordEncoder.matches(ACTUAL, usuario.getPassword())).isFalse();
        assertThat(usuario.getVersionCredenciales()).isEqualTo(1);
    }

    @Test
    @DisplayName("Rechaza el cambio si la contraseña actual es incorrecta, si la nueva es igual o es débil")
    void rechazaCambios() {
        Usuario usuario = usuario(7, true);
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> servicio.cambiarPassword(7, new CambioPasswordRequest("Otra#2026x", NUEVA), sesion(7)))
                .isInstanceOf(PasswordInvalidaException.class).hasMessageContaining("actual es incorrecta");
        assertThatThrownBy(() -> servicio.cambiarPassword(7, new CambioPasswordRequest(ACTUAL, ACTUAL), sesion(7)))
                .isInstanceOf(PasswordInvalidaException.class).hasMessageContaining("distinta");
        assertThatThrownBy(
                () -> servicio.cambiarPassword(7, new CambioPasswordRequest(ACTUAL, "sinespeciales1A"), sesion(7)))
                .isInstanceOf(PasswordInvalidaException.class).hasMessageContaining("carácter especial");
        assertThat(usuario.getVersionCredenciales()).isZero();
    }

    @Test
    @DisplayName("Un usuario inactivo no puede cambiar su contraseña")
    void inactivo() {
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario(7, false)));
        assertThatThrownBy(() -> servicio.cambiarPassword(7, new CambioPasswordRequest(ACTUAL, NUEVA), sesion(7)))
                .isInstanceOf(UsuarioInactivoException.class);
    }

    @Test
    @DisplayName("Sincroniza el correo y se inactiva cuando el cliente se da de baja")
    void sincronizaEInactiva() {
        Usuario usuario = usuario(7, true);
        when(usuarioRepository.findByClienteId(7)).thenReturn(Optional.of(usuario));
        Cliente cliente = new Cliente("GOMC900515HQTMNR08", "GOMC900515AB7");
        ReflectionTestUtils.setField(cliente, "id", 7);
        cliente.setCorreo("nuevo@prueba.mx");

        servicio.sincronizarCorreo(cliente);
        servicio.inactivarPorBajaDeCliente(7);

        assertThat(usuario.getCorreo()).isEqualTo("nuevo@prueba.mx");
        assertThat(usuario.isActivo()).isFalse();
    }
}
